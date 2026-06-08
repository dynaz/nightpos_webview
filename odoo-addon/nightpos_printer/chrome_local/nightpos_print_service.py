import sys

try:
    import win32event
    import win32service
    import win32serviceutil
    import servicemanager
except ImportError:
    print("ERROR: pywin32 is required for Windows service support. Run: pip install pywin32")
    sys.exit(1)

import local_print_server


class NightPOSPrintService(win32serviceutil.ServiceFramework):
    _svc_name_ = "NightPOSPrintServer"
    _svc_display_name_ = "NightPOS Print Server"
    _svc_description_ = "NightPOS local print server for NightPOS Chrome extension."

    def __init__(self, args):
        win32serviceutil.ServiceFramework.__init__(self, args)
        self.hWaitStop = win32event.CreateEvent(None, 0, 0, None)

    def SvcStop(self):
        self.ReportServiceStatus(win32service.SERVICE_STOP_PENDING)
        win32event.SetEvent(self.hWaitStop)

    def SvcDoRun(self):
        servicemanager.LogInfoMsg("NightPOS Print Server service starting...")
        local_print_server.run_service(self.hWaitStop)
        servicemanager.LogInfoMsg("NightPOS Print Server service stopped.")


if __name__ == "__main__":
    win32serviceutil.HandleCommandLine(NightPOSPrintService)
