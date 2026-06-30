@rem
@rem Gradle 启动包装器脚本
@rem 用于 Windows 系统
@rem

@if "%DEBUG%"=="" @echo off
@rem 设置本地范围，仅限带有 setlocal 的批处理文件

set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem 查找 java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
set APP_BASE_NAME=%~n0
set APP_HOME=%~dp0

@rem 增加 classpath 中最大打开文件描述符数
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

@rem 执行 Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
@rem 退出代码
if "%OS%"=="Windows_NT" exit /b %ERRORLEVEL%
exit /b %ERRORLEVEL%

:fail
rem 设置退出代码的辅助变量
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%GRADLE_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%
