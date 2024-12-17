@echo off
set JAVAFX_PATH=C:\Java\openjfx-21.0.5_windows-x64_bin-sdk\javafx-sdk-21.0.5\lib
set MYSQL_PATH=C:\Java\mysql-connector-j-9.1.0\mysql-connector-j-9.1.0.jar
set POI_PATH=C:\Java\poi-5.2.3.jar
set POI_OOXML_PATH=C:\Java\poi-ooxml-5.2.3.jar
set ITEXT_PATH=C:\Java\itextpdf-5.5.13.3.jar

:: JavaMail dependencies (version 1.6.2)
set JAVAX_MAIL_PATH=lib\javax.mail-1.6.2.jar
set ACTIVATION_PATH=lib\javax.activation-1.2.0.jar

:: FontAwesomeFX dependencies
set FONTAWESOME_PATH=lib\fontawesomefx-8.9.jar
set FONTAWESOME_COMMONS_PATH=lib\fontawesomefx-commons-8.15.jar
set FONTAWESOME_ICONS_PATH=lib\fontawesomefx-fontawesome-4.7.0-9.1.2.jar

:: Set classpath for all dependencies
set CLASSPATH=^
%MYSQL_PATH%;^
%POI_PATH%;^
%POI_OOXML_PATH%;^
%ITEXT_PATH%;^
%JAVAX_MAIL_PATH%;^
%ACTIVATION_PATH%;^
%FONTAWESOME_PATH%;^
%FONTAWESOME_COMMONS_PATH%;^
%FONTAWESOME_ICONS_PATH%;^
src\main\java

echo Verifying dependencies...
if not exist "%MYSQL_PATH%" (
    echo Error: MySQL connector not found at %MYSQL_PATH%
    echo Please ensure mysql-connector-j-9.1.0.jar is present at the specified location
    pause
    exit /b 1
)

if not exist "%POI_PATH%" (
    echo Error: Apache POI not found at %POI_PATH%
    echo Please ensure poi-5.2.3.jar is present at the specified location
    echo Download from: https://archive.apache.org/dist/poi/release/bin/poi-bin-5.2.3-20220909.zip
    pause
    exit /b 1
)

if not exist "%POI_OOXML_PATH%" (
    echo Error: Apache POI OOXML not found at %POI_OOXML_PATH%
    echo Please ensure poi-ooxml-5.2.3.jar is present at the specified location
    echo Download from: https://archive.apache.org/dist/poi/release/bin/poi-bin-5.2.3-20220909.zip
    pause
    exit /b 1
)

if not exist "%ITEXT_PATH%" (
    echo Error: iText PDF not found at %ITEXT_PATH%
    echo Please ensure itextpdf-5.5.13.3.jar is present at the specified location
    echo Download from: https://github.com/itext/itextpdf/releases/download/5.5.13.3/itextpdf-5.5.13.3.jar
    pause
    exit /b 1
)

if not exist "%JAVAX_MAIL_PATH%" (
    echo Error: JavaMail 1.6.2 not found at %JAVAX_MAIL_PATH%
    echo Please ensure javax.mail-1.6.2.jar is present in the lib directory
    pause
    exit /b 1
)

if not exist "%ACTIVATION_PATH%" (
    echo Error: JavaBeans Activation Framework 1.2.0 not found at %ACTIVATION_PATH%
    echo Please ensure javax.activation-1.2.0.jar is present in the lib directory
    pause
    exit /b 1
)

if not exist "%FONTAWESOME_PATH%" (
    echo Error: FontAwesomeFX not found at %FONTAWESOME_PATH%
    echo Please ensure fontawesomefx-8.9.jar is present in the lib directory
    pause
    exit /b 1
)

if not exist "%FONTAWESOME_COMMONS_PATH%" (
    echo Error: FontAwesomeFX Commons not found at %FONTAWESOME_COMMONS_PATH%
    echo Please ensure fontawesomefx-commons-8.15.jar is present in the lib directory
    pause
    exit /b 1
)

if not exist "%FONTAWESOME_ICONS_PATH%" (
    echo Error: FontAwesomeFX Icons not found at %FONTAWESOME_ICONS_PATH%
    echo Please ensure fontawesomefx-fontawesome-4.7.0-9.1.2.jar is present in the lib directory
    pause
    exit /b 1
)

echo Creating class directory...
if not exist "target\classes" mkdir target\classes

echo Compiling...
javac -d target\classes -cp "%CLASSPATH%" --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml ^
src\main\java\com\evsu\violation\Main.java ^
src\main\java\com\evsu\violation\controllers\*.java ^
src\main\java\com\evsu\violation\models\*.java ^
src\main\java\com\evsu\violation\util\*.java

if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Copying resources...
xcopy /s /y "src\main\resources" "target\classes"

echo Running with JavaMail debug enabled...
java -cp "target\classes;%CLASSPATH%" ^
-Djavax.net.debug=all ^
-Dmail.debug=true ^
-Dmail.debug.auth=true ^
-Dmail.socket.debug=true ^
--module-path "%JAVAFX_PATH%" ^
--add-modules=javafx.controls,javafx.fxml,javafx.graphics ^
--add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED ^
--add-exports javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED ^
--add-exports javafx.graphics/com.sun.javafx.util=ALL-UNNAMED ^
--add-exports javafx.base/com.sun.javafx.reflect=ALL-UNNAMED ^
--add-exports javafx.base/com.sun.javafx.beans=ALL-UNNAMED ^
--add-exports javafx.graphics/com.sun.glass.utils=ALL-UNNAMED ^
--add-exports javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED ^
--add-exports javafx.graphics/com.sun.javafx.css=ALL-UNNAMED ^
--add-exports javafx.base/com.sun.javafx.runtime=ALL-UNNAMED ^
--add-exports javafx.base/com.sun.javafx.collections=ALL-UNNAMED ^
--add-exports javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED ^
--add-exports javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED ^
--add-exports javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED ^
--add-exports javafx.graphics/com.sun.javafx.scene.text=ALL-UNNAMED ^
--add-exports javafx.graphics/com.sun.javafx.geom=ALL-UNNAMED ^
main.java.com.evsu.violation.Main

pause 