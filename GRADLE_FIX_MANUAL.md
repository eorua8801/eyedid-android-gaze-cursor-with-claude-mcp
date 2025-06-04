# EyedidTracker Gradle Cache Fix - Manual Commands

## Option 1: Run the English batch file
```
fix_gradle_cache_en.bat
```

## Option 2: Manual commands (copy-paste one by one)

### Step 1: Navigate to project
```
cd C:\Users\1109e\AndroidStudioProjects\EyedidTracker-Refactored
```

### Step 2: Clean project
```
gradlew clean
```

### Step 3: Stop Gradle daemon
```
gradlew --stop
```

### Step 4: Remove corrupted caches (run each line separately)
```
rmdir /s /q "%USERPROFILE%\.gradle\caches\8.11.1\transforms"
rmdir /s /q "%USERPROFILE%\.gradle\caches\modules-2"
rmdir /s /q "%USERPROFILE%\.gradle\daemon"
rmdir /s /q ".gradle"
rmdir /s /q "app\build"
```

### Step 5: Regenerate wrapper
```
gradlew wrapper --gradle-version=8.11.1 --distribution-type=all
```

### Step 6: Test build
```
gradlew tasks
```

## Expected Result
- No error messages
- "BUILD SUCCESSFUL" at the end
- Ready for Android Studio sync

## If Still Fails
Report the exact error message to the autonomous agent for immediate alternative solution.
