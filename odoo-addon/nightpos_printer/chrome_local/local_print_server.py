#!/usr/bin/env python3
"""
NightPOS Local Print Server
Listens on ws://localhost:8765 for ESC/POS print jobs from the Chrome extension.

Supported actions:
  ping            → { "success": true, "version": "1.0.0" }
  list_printers   → { "success": true, "printers": [...] }
  print           → { "success": true }  or  { "success": false, "error": "..." }
"""

import asyncio
import base64
import json
import logging
import platform
import sys

try:
    import websockets
    from websockets.server import serve
except ImportError:
    print("ERROR: 'websockets' package not found. Run: pip install websockets")
    sys.exit(1)

HOST    = "localhost"
PORT    = 8765
VERSION = "1.0.0"

logging.basicConfig(
    level=logging.INFO,
    format="[NightPOS] %(asctime)s %(levelname)s %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger("nightpos")

# ── Platform printing ─────────────────────────────────────────────────────────

SYSTEM = platform.system()  # "Windows" | "Linux" | "Darwin"


def list_printers():
    """Return a list of installed printer names."""
    if SYSTEM == "Windows":
        return _list_printers_windows()
    elif SYSTEM == "Linux":
        return _list_printers_linux()
    else:
        return _list_printers_linux()  # macOS also uses lpstat


def _list_printers_windows():
    try:
        import win32print
        return [p[2] for p in win32print.EnumPrinters(
            win32print.PRINTER_ENUM_LOCAL | win32print.PRINTER_ENUM_CONNECTIONS,
            None, 1
        )]
    except ImportError:
        log.warning("win32print not available — install pywin32")
        return []
    except Exception as e:
        log.error("list_printers error: %s", e)
        return []


def _list_printers_linux():
    import subprocess
    try:
        out = subprocess.check_output(
            ["lpstat", "-a"], text=True, stderr=subprocess.DEVNULL
        )
        return [line.split()[0] for line in out.strip().splitlines() if line]
    except Exception:
        try:
            # Fallback: read CUPS directly
            out = subprocess.check_output(
                ["lpstat", "-p"], text=True, stderr=subprocess.DEVNULL
            )
            printers = []
            for line in out.strip().splitlines():
                if line.startswith("printer "):
                    printers.append(line.split()[1])
            return printers
        except Exception as e:
            log.error("list_printers error: %s", e)
            return []


def send_raw_to_printer(printer_name: str, raw_bytes: bytes):
    """Send raw bytes to a named printer."""
    if SYSTEM == "Windows":
        _print_windows(printer_name, raw_bytes)
    else:
        _print_linux(printer_name, raw_bytes)


def _print_windows(printer_name: str, raw_bytes: bytes):
    try:
        import win32print
        import win32ui
    except ImportError:
        raise RuntimeError(
            "pywin32 is not installed. Run: pip install pywin32"
        )

    # Resolve default printer if none specified
    if not printer_name:
        printer_name = win32print.GetDefaultPrinter()
        log.info("Using default printer: %s", printer_name)

    printer_handle = win32print.OpenPrinter(printer_name)
    try:
        job = win32print.StartDocPrinter(printer_handle, 1, ("ESC/POS Job", None, "RAW"))
        try:
            win32print.StartPagePrinter(printer_handle)
            win32print.WritePrinter(printer_handle, raw_bytes)
            win32print.EndPagePrinter(printer_handle)
        finally:
            win32print.EndDocPrinter(printer_handle)
    finally:
        win32print.ClosePrinter(printer_handle)

    log.info("Sent %d bytes to Windows printer '%s'", len(raw_bytes), printer_name)


def _print_linux(printer_name: str, raw_bytes: bytes):
    import subprocess, tempfile, os

    with tempfile.NamedTemporaryFile(delete=False, suffix=".bin") as tmp:
        tmp.write(raw_bytes)
        tmp_path = tmp.name

    try:
        cmd = ["lp", "-o", "raw"]
        if printer_name:
            cmd += ["-d", printer_name]
        cmd.append(tmp_path)
        result = subprocess.run(cmd, capture_output=True, text=True)
        if result.returncode != 0:
            raise RuntimeError(result.stderr.strip() or "lp command failed")
        log.info("Sent %d bytes to Linux printer '%s'", len(raw_bytes), printer_name or "(default)")
    finally:
        os.unlink(tmp_path)


# ── WebSocket handler ─────────────────────────────────────────────────────────

async def handle(websocket):
    peer = websocket.remote_address
    log.info("Client connected: %s", peer)
    try:
        async for raw in websocket:
            try:
                msg = json.loads(raw)
            except json.JSONDecodeError:
                await websocket.send(json.dumps({"success": False, "error": "Invalid JSON"}))
                continue

            action = msg.get("action", "")
            log.info("Action: %s", action)

            if action == "ping":
                await websocket.send(json.dumps({"success": True, "version": VERSION}))

            elif action == "list_printers":
                printers = list_printers()
                await websocket.send(json.dumps({"success": True, "printers": printers}))

            elif action == "print":
                printer_name = msg.get("printer_name", "")
                raw_data_b64  = msg.get("raw_data", "")

                if not raw_data_b64:
                    await websocket.send(json.dumps({"success": False, "error": "raw_data is empty"}))
                    continue

                try:
                    raw_bytes = base64.b64decode(raw_data_b64)
                except Exception as e:
                    await websocket.send(json.dumps({"success": False, "error": f"base64 decode failed: {e}"}))
                    continue

                try:
                    send_raw_to_printer(printer_name, raw_bytes)
                    await websocket.send(json.dumps({"success": True}))
                except Exception as e:
                    log.error("Print failed: %s", e)
                    await websocket.send(json.dumps({"success": False, "error": str(e)}))

            else:
                await websocket.send(json.dumps({"success": False, "error": f"Unknown action: {action}"}))

    except websockets.exceptions.ConnectionClosedOK:
        pass
    except websockets.exceptions.ConnectionClosedError as e:
        log.warning("Connection closed with error: %s", e)
    finally:
        log.info("Client disconnected: %s", peer)


# ── Entry point ───────────────────────────────────────────────────────────────

async def _wait_stop_event(stop_handle):
    try:
        import win32event
    except ImportError:
        log.warning("pywin32 not available; service stop may not work cleanly.")
        return

    while True:
        rc = win32event.WaitForSingleObject(stop_handle, 500)
        if rc == win32event.WAIT_OBJECT_0:
            log.info("Service stop requested.")
            return
        await asyncio.sleep(0.1)


async def main(stop_handle=None):
    log.info("NightPOS Print Server v%s starting on ws://%s:%d", VERSION, HOST, PORT)
    log.info("Platform: %s", SYSTEM)
    printers = list_printers()
    if printers:
        log.info("Detected printers: %s", ", ".join(printers))
    else:
        log.warning("No printers detected (or platform tools unavailable)")

    async with serve(handle, HOST, PORT) as server:
        log.info("Ready. Waiting for connections… (Ctrl+C to stop)")
        if stop_handle is None:
            await server.wait_closed()
        else:
            await _wait_stop_event(stop_handle)


def run_service(stop_handle):
    try:
        asyncio.run(main(stop_handle))
    except Exception as exc:
        log.error("Service terminated with error: %s", exc)
        raise


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        log.info("Server stopped.")
