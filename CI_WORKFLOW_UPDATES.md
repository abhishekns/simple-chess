# Android CI Workflow Updates Required

## Issue
The GitHub App doesn't have `workflows` permission to update `.github/workflows/android-ci.yml` directly. The following changes need to be applied manually to fix the Java version compatibility issue.

## Required Changes to `.github/workflows/android-ci.yml`

### 1. Update Java Version (Line 18-22)
**Change from:**
```yaml
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        distribution: 'temurin'
        java-version: '11' # Android Gradle Plugin compatibility
```

**Change to:**
```yaml
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        distribution: 'temurin'
        java-version: '17' # Updated to Java 17 for Android SDK compatibility
```

### 2. Add Environment Variables (After line 14)
**Add this section:**
```yaml
    env:
      ANDROID_SDK_ROOT: ${{ github.workspace }}/android-sdk
      JAVA_HOME: ${{ runner.temp }}/java_home
```

### 3. Add Java Version Verification (After Java setup step)
**Add this step:**
```yaml
    - name: Verify Java version
      run: |
        echo "Java version:"
        java -version
        echo "JAVA_HOME: $JAVA_HOME"
```

### 4. Add Android SDK Verification (After SDK installation step)
**Add this step:**
```yaml
    - name: Verify Android SDK installation
      run: |
        echo "Android SDK Root: $ANDROID_SDK_ROOT"
        ls -la $ANDROID_SDK_ROOT
        echo "SDK Manager version:"
        $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --version
```

### 5. Add Gradle Caching (Before test execution)
**Add this step:**
```yaml
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
```

### 6. Update Gradle Commands
**Change all gradle commands to include flags:**
- `./gradlew testDebugUnitTest` → `./gradlew testDebugUnitTest --console=plain --no-daemon`
- `./gradlew assembleDebug` → `./gradlew assembleDebug --console=plain --no-daemon`
- `./gradlew lintDebug` → `./gradlew lintDebug --console=plain --no-daemon`

### 7. Add Test Results Upload (After test execution)
**Add this step:**
```yaml
    - name: Upload test results
      uses: actions/upload-artifact@v3
      if: always()
      with:
        name: test-results
        path: P2PChessApp/app/build/reports/tests/testDebugUnitTest/
```

### 8. Enable Lint Results Upload
**Uncomment and modify the lint results upload:**
```yaml
    - name: Upload Lint results
      uses: actions/upload-artifact@v3
      if: always()
      with:
        name: lint-results
        path: P2PChessApp/app/build/reports/lint-results-debug.html
```

### 9. Update Documentation Comments
**Update the note section:**
```yaml
# 2. Updated to use Java 17 for compatibility with newer Android SDK tools.
#    The Android SDK manager requires Java 17 (class file version 61.0).
```

## Why These Changes Are Needed

### Java 17 Requirement
- **Error**: `UnsupportedClassVersionError: class file version 61.0`
- **Cause**: Android SDK tools now require Java 17 minimum
- **Solution**: Update CI to use Java 17 instead of Java 11

### Additional Improvements
- **Environment Variables**: Better SDK path management
- **Verification Steps**: Help debug issues in CI
- **Gradle Caching**: Improve build performance
- **Artifact Uploads**: Preserve test results and reports
- **Better Logging**: `--console=plain --no-daemon` for clearer CI output

## Manual Update Process

1. Navigate to `.github/workflows/android-ci.yml` in the repository
2. Apply the changes listed above
3. Commit the changes with message: "Update Android CI to use Java 17 for SDK compatibility"
4. The next CI run should work without the Java version error

## Expected Results After Update

✅ Android SDK installation succeeds with Java 17  
✅ All 72 unit tests pass (100% success rate)  
✅ Debug APK builds successfully  
✅ Lint analysis completes  
✅ Test results and reports uploaded as artifacts  

## Alternative: Complete Workflow File

If easier, replace the entire `.github/workflows/android-ci.yml` file with the complete updated version available in the local changes (see `git diff` output for full file).