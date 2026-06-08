# NightPOS Local Printer

Direct ESC/POS printing from Odoo / NightPOS to local thermal printers — no print dialog, no IoBox required.

```
[Odoo page]  →  Chrome Extension (content.js)  →  WebSocket ws://localhost:8765  →  local_print_server.py  →  Printer
```

---

## Components

| Component | Location | Purpose |
|---|---|---|
| Chrome Extension | `chrome_extension/` | Intercepts print events on the Odoo page, forwards to Python server |
| Python Print Server | `local_print_server.py` | Receives print jobs, sends raw bytes to the Windows/Linux printer |
| Odoo Add-on | `../ (nightpos_printer)` | Dispatches `nightpos_local_print_event` from Python backend |

---

## 1 — Install the Python Print Server (Windows)

**Requirements:** Python 3.9+

```
cd nightpos_printer
install.bat        # creates .venv and installs websockets + pywin32
```

**Start the server:**
```
start_server.bat
```

The server starts at `ws://localhost:8765` and logs to the console.

## 1.1 — Build a standalone EXE and install as a Windows service

To create a standalone server executable and Windows service wrapper:

```bat
build_exe.bat
```

This builds:
- `NightPOS_PrintServer.exe` — standalone console server
- `NightPOS_PrintServer_Service.exe` — Windows service installer

To install and start the service (run as Administrator):

```bat
install_service.bat
```

To stop and remove the service:

```bat
uninstall_service.bat
```

**Manual install (any OS):**
```bash
pip install websockets pywin32   # pywin32 is Windows-only
python local_print_server.py
```

---

## 2 — Load the Chrome Extension

1. Open Chrome and go to `chrome://extensions`
2. Enable **Developer mode** (top-right toggle)
3. Click **Load unpacked**
4. Select the `chrome_extension/` folder inside this directory
5. The NightPOS Printer icon appears in the toolbar

---

## 3 — Configure the Extension

Click the toolbar icon to open the popup:

- **Server URL** — defaults to `ws://localhost:8765`. Change and click **Save** if you run the server on a different port.
- **Status indicator** — shows green when the Python server is reachable.
- **Available printers** — lists all printers visible to the server.

---

## 4 — How printing works

When Odoo dispatches a print event:

```js
window.dispatchEvent(new CustomEvent('nightpos_local_print_event', {
    detail: {
        printer_name: 'EPSON_TM_T82',   // must match Windows printer name
        raw_data: '<base64-encoded ESC/POS bytes>'
    }
}));
```

The extension `content.js` picks it up, opens a WebSocket to the server, and sends:

```json
{ "action": "print", "printer_name": "EPSON_TM_T82", "raw_data": "..." }
```

The server decodes the base64 and sends raw bytes to the printer via `win32print` (Windows) or `lp` (Linux).

---

## 5 — Supported WebSocket actions

| Action | Request | Response |
|---|---|---|
| `ping` | `{"action":"ping"}` | `{"success":true,"version":"1.0.0"}` |
| `list_printers` | `{"action":"list_printers"}` | `{"success":true,"printers":["..."]}` |
| `print` | `{"action":"print","printer_name":"...","raw_data":"..."}` | `{"success":true}` or `{"success":false,"error":"..."}` |

---

## 6 — Printer name

The `printer_name` must match exactly the Windows printer name shown in **Settings → Printers**.

- Leave it empty (`""`) to use the system default printer.
- The extension popup lists all available printers so you can copy the exact name.

---

## 7 — Troubleshooting

| Problem | Fix |
|---|---|
| Badge shows `ERR` | Open the popup — check server status and last error |
| Server not starting | Make sure Python 3.9+ is installed and `install.bat` ran without errors |
| `win32print` not found | Run `.venv\Scripts\pip install pywin32` |
| Printer not found | Verify the printer name in Windows Settings → Printers & scanners |
| Extension not responding | Reload the extension at `chrome://extensions` |
| Port 8765 in use | Edit `local_print_server.py` — change `PORT = 8765` and update the extension URL |

---

## 8 — Auto-start on Windows login (optional)

Create a shortcut to `start_server.bat` and place it in:
```
%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup
```

Or create a Windows Task Scheduler task to run `start_server.bat` at login.
