# Manual Workflow Changes Required

## Issue
The GitHub workflow changes couldn't be pushed automatically due to GitHub's security policy that prevents GitHub Apps from modifying workflow files without `workflows` permission.

## Required Changes to `.github/workflows/android-ci.yml`

You need to manually apply these changes to the Android CI workflow file:

### 1. Change JDK Version (Line 17-21)
```yaml
# BEFORE:
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        distribution: 'temurin'
        java-version: '11' # Android Gradle Plugin compatibility

# AFTER:
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        distribution: 'temurin'
        java-version: '17' # Modern Android Gradle Plugin compatibility
```

### 2. Add Java Verification Step (Insert after JDK setup)
```yaml
    - name: Verify Java setup
      run: |
        echo "JAVA_HOME: $JAVA_HOME"
        echo "Java version:"
        java -version
        echo "Javac version:"
        javac -version
```

### 3. Update Comments (Lines 85-86)
```yaml
# BEFORE:
# 2. Ensure your project uses Java 11 or configure the JDK version accordingly.
#    Newer Android Gradle Plugins might require JDK 17. Adjust as needed.

# AFTER:
# 2. This workflow uses JDK 17, which is compatible with modern Android Gradle Plugins.
#    The setup.sh script uses the Java version provided by the CI environment to avoid conflicts.
```

## Complete Diff
Here's the complete diff to apply:

```diff
-    - name: Set up JDK 11
+    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        distribution: 'temurin' # Popular OpenJDK distribution
        java-version: '17' # Modern Android Gradle Plugin compatibility

    - name: Verify Java setup
      run: |
        echo "JAVA_HOME: $JAVA_HOME"
        echo "Java version:"
        java -version
        echo "Javac version:"
        javac -version

-# 2. Ensure your project uses Java 11 or configure the JDK version accordingly.
-#    Newer Android Gradle Plugins might require JDK 17. Adjust as needed.
+# 2. This workflow uses JDK 17, which is compatible with modern Android Gradle Plugins.
+#    The setup.sh script uses the Java version provided by the CI environment to avoid conflicts.
```

## Why These Changes Are Important
1. **Fixes the Java certificate issue**: Using consistent JDK 17 prevents ca-certificates-java conflicts
2. **Modern compatibility**: JDK 17 is required for newer Android Gradle Plugin versions
3. **Better debugging**: The verification step helps identify Java setup issues
4. **Consistency**: Matches the environment-aware setup.sh script changes

## Status
- ✅ **setup.sh** - Updated and pushed successfully
- ✅ **SETUP_FIX_SUMMARY.md** - Documentation created and pushed
- ⏳ **android-ci.yml** - Manual changes required (this file)

Once you apply these workflow changes, the Android CI will use JDK 17 consistently and avoid the Java certificate setup conflicts.