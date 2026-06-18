# Optional: Set GraalVM path here if you don't want to change global JAVA_HOME
$env:GRAALVM_HOME = "C:\dev\graalvm-jdk-21.0.9+7.1"

# If GRAALVM_HOME is set, use it to configure environment for this session only
if ($env:GRAALVM_HOME) {
    Write-Host "Configuring GraalVM..."
    if (!(Test-Path $env:GRAALVM_HOME)) {
        Write-Error "Path not found: $env:GRAALVM_HOME"
        Write-Error "Please check if the directory exists."
        exit 1
    }
    
    $binPath = "$env:GRAALVM_HOME\bin"
    if (!(Test-Path $binPath)) {
        Write-Error "Bin directory not found: $binPath"
        Write-Error "It seems the path is incorrect. Did you unzip it into a nested folder?"
        Write-Error "Try looking inside $env:GRAALVM_HOME to see if there is another folder."
        # List directories to help user
        Get-ChildItem $env:GRAALVM_HOME | Select-Object Name
        exit 1
    }

    Write-Host "Setting JAVA_HOME to $env:GRAALVM_HOME"
    $env:JAVA_HOME = $env:GRAALVM_HOME
    $env:Path = "$binPath;$env:Path"
}

# Debug: Check java version
Write-Host "Checking Java version..."
java -version

# Check if java version contains "GraalVM" or "native-image" is available
$javaVersion = java -version 2>&1
if ($javaVersion -notmatch "GraalVM" -and !(Get-Command native-image -ErrorAction SilentlyContinue)) {
    Write-Warning "It seems you are not using GraalVM or 'native-image' is not in your PATH."
    Write-Warning "Current Java Version Output: $javaVersion"
    Write-Warning "Please set `$env:GRAALVM_HOME in this script correctly."
    exit 1
}

echo "Building Native Image..."
echo "This may take a few minutes..."

# Run Maven with native profile
mvn -Pnative native:compile -DskipTests

echo "Done!"
echo "Executable should be in the 'target' directory."
