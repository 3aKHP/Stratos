@rem Minimal gradlew — delegates to local Gradle installation if wrapper jar not available
@if "%DEBUG%"=="" @echo off
@rem Find project root
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.

set GRADLE_USER_HOME=%USERPROFILE%\.gradle

@rem Use locally installed Gradle 8.14 if available (bypasses wrapper jar dependency)
set LOCAL_GRADLE=%GRADLE_USER_HOME%\wrapper\dists\gradle-8.14-bin\38aieal9i53h9rfe7vjup95b9\gradle-8.14\bin\gradle
if exist "%LOCAL_GRADLE%.bat" (
    "%LOCAL_GRADLE%.bat" %*
    goto :eof
)

@rem Fallback: try to use Android Studio's bundled Gradle
for /d %%i in ("E:\AndroidStudio\plugins\gradle\*") do (
    if exist "%%i\bin\gradle.bat" (
        "%%i\bin\gradle.bat" %*
        goto :eof
    )
)

@rem Last resort: print instructions
echo Gradle wrapper not found. Please open this project in Android Studio to set up the wrapper.
echo File ^> Open ^> select %DIRNAME%
exit /b 1
