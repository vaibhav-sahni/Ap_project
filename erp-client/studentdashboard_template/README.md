# Student Dashboard Template

This folder contains a **backup template** of the student dashboard that you can use to create multiple dashboard instances or variations.

## Purpose

Keep this as a clean copy to:

- Create new dashboard variations (e.g., instructor dashboard, admin dashboard)
- Experiment with different layouts without affecting the original
- Quickly restore if you break something in the main dashboard

## How to Use

### Option 1: Create a New Dashboard Variation

```powershell
# Copy the template to create a new dashboard type
Copy-Item -Path "studentdashboard_template" -Destination "src\main\java\edu\univ\erp\ui\instructordashboard" -Recurse

# Then rename packages inside the files from 'studentdashboard' to 'instructordashboard'
```

### Option 2: Restore Original Dashboard

```powershell
# If you break something, restore from this template
Remove-Item -Path "src\main\java\edu\univ\erp\ui\studentdashboard" -Recurse -Force
Copy-Item -Path "studentdashboard_template" -Destination "src\main\java\edu\univ\erp\ui\studentdashboard" -Recurse
```

### Option 3: Run Multiple Instances

You can run multiple dashboard windows by:

1. Running the student dashboard: `mvn exec:java "-Dexec.mainClass=application.Application"`
2. The application can open multiple windows programmatically

## Important Notes

- **DO NOT** move this template back into the `src/main/java` folder - it will cause duplicate class compilation errors
- Keep this folder at the project root level
- Always update this template when you make improvements to the main dashboard

## Current Features

- FlatLaf theme with smooth animations
- Drawer menu with icons
- Dashboard with charts (pie, line, bar)
- Form management system
- User profile display
- Theme switcher
