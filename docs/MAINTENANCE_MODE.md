# Maintenance Mode

## Server

- Toggle: `TOGGLE_MAINTENANCE:ON|OFF`
- Check: `CHECK_MAINTENANCE` -> `SUCCESS:true|false`
- Enforced in services to block certain write operations when ON.

## Client

- `MaintenanceModeManager` polls `CHECK_MAINTENANCE` and registers app windows.
- Shows a red toast at top-center via `ToastNotification.showMaintenanceNotification(window, true)`.
- Reappears on window focus and in-dashboard form switches; cooldown prevents spam.

## Customization

- Edit toast styles/colors in `ToastNotification`.
- Adjust poll interval/cooldown in `MaintenanceModeManager`.
