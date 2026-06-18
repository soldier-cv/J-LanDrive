# Ensure the project is built
mvn clean package -DskipTests

# Define variables
$APP_NAME = "J-LanDrive"
$APP_VERSION = "1.0.0"
$INPUT_DIR = "target"
$MAIN_JAR = "j-landrive-0.0.1-SNAPSHOT.jar"
$OUTPUT_DIR = "dist"
# Ensure the project is built
mvn clean package -DskipTests

# Define variables
$APP_NAME = "J-LanDrive"
$APP_VERSION = "1.0.0"
$INPUT_DIR = "target"
$MAIN_JAR = "j-landrive-0.0.1-SNAPSHOT.jar"
$OUTPUT_DIR = "dist"

# Remove previous build
if (Test-Path $OUTPUT_DIR) {
    Remove-Item -Recurse -Force $OUTPUT_DIR
}

# Run jpackage
# This creates a standalone executable folder (app-image)
# To create an installer (exe/msi), change --type to "exe" or "msi"
echo "Packaging $APP_NAME..."

# To create an installer (exe/msi), you MUST install WiX Toolset (https://wixtoolset.org/)
# and change --type to "exe" or "msi".
# "app-image" creates a standalone folder with an executable inside (no WiX required).
jpackage `
  --name $APP_NAME `
  --app-version $APP_VERSION `
  --input $INPUT_DIR `
  --main-jar $MAIN_JAR `
  --type "app-image" `
  --dest $OUTPUT_DIR `
  --win-console `
  --java-options "-Dfile.encoding=UTF-8"

echo "Done! Executable is in $OUTPUT_DIR/$APP_NAME"
