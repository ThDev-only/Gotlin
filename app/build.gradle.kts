import java.io.File
import java.net.URL


plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "com.thdev.gotlin"
    compileSdk = 33
    
    defaultConfig {
        applicationId = "com.thdev.gotlin"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        
        vectorDrawables { 
            useSupportLibrary = true
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
        
    }
    
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "11"
}


tasks.register<Exec>("generateGoSharedLibs") {

    dependsOn("installGoMobile")
    description = "Generate shared libraries using gomobile"
    group = "build"

    val goDir = "${projectDir}/src/main/go"
    val outputDir = "${buildDir}/intermediates/go-libs"

    doFirst {
        // Retrieve the gomobile path from the global variable
        val gomobilePath = project.extra["gomobilePath"] as? String
            ?: throw GradleException("gomobile path not set. Run 'installGoMobile' first.")

        if (!File(gomobilePath).exists()) {
            throw GradleException("gomobile binary not found at $gomobilePath. Ensure it is installed and try again.")
        }

        mkdir(outputDir)

        // Set the command line for execution
        commandLine(gomobilePath, "bind", "-target=android", "-o", outputDir, goDir)
    }
}

tasks.register("installGo") {
    doLast {
        val goVersion = "1.23.5" // Define the desired Go version

        // Detect the operating system and architecture
        val os = System.getProperty("os.name").toLowerCase()
        val arch = System.getProperty("os.arch").toLowerCase()

        // Determine the Go binary URL based on the system
        val goBinary: String = when {
            os.contains("linux") && arch.contains("amd64") -> "go$goVersion.linux-amd64.tar.gz"
            os.contains("linux") && arch.contains("aarch64") -> "go$goVersion.linux-arm64.tar.gz"
            os.contains("mac") && arch.contains("amd64") -> "go$goVersion.darwin-amd64.tar.gz"
            os.contains("mac") && arch.contains("aarch64") -> "go$goVersion.darwin-arm64.tar.gz"
            os.contains("windows") && arch.contains("amd64") -> "go$goVersion.windows-amd64.zip"
            else -> {
                println("Unsupported OS or architecture: $os, $arch")
                return@doLast
            }
        }

        val url = "https://golang.org/dl/$goBinary"

        // Define the directory where Go will be installed
        val installDir = File(System.getProperty("user.home"), "")
        if (!installDir.exists()) {
            installDir.mkdirs()
        }

        // Installation file path
        val goFile = File(installDir, "$goVersion.tar.gz")
        val goBinDir = File(installDir, "go/bin")

        // Check if Go is already installed
        if (goBinDir.exists() && goBinDir.isDirectory) {
            println("Go $goVersion is already installed at: ${installDir.absolutePath}")
            return@doLast
        }

        // Download Go
        println("Downloading Go $goVersion for $os ($arch)...")
        val download = URL(url).openStream()
        goFile.outputStream().use { outputStream ->
            download.copyTo(outputStream)
        }

        // Extract Go
        println("Extracting Go...")
        val tarCommand = when {
            os.contains("windows") -> "powershell -Command Expand-Archive -Path ${goFile.absolutePath} -DestinationPath ${installDir.absolutePath}"
            else -> "tar -C ${installDir.absolutePath} -xzf ${goFile.absolutePath}"
        }
        val process = Runtime.getRuntime().exec(tarCommand)
        process.waitFor()

        println("Go $goVersion successfully installed at: ${installDir.absolutePath}")
    }
}

tasks.register("installGoMobile") {
    doLast {
        
        dependsOn("installGo")
        val goVersion = "1.23.5" // Define the desired Go version
        val goPath = File(System.getProperty("user.home"), "")

        // Check if Go is installed
        val goBinDir = File(goPath, "go/bin")
        if (!goBinDir.exists() || !goBinDir.isDirectory) {
            println("Go is not correctly installed. Run the 'installGo' task first.")
            return@doLast
        }

        val goBinPath = File(goBinDir, "go").absolutePath

        println("Checking if Go is accessible...")
        val goVersionCommand = ProcessBuilder(goBinPath, "version")
        val goVersionProcess = goVersionCommand.start()

        // Get standard output and error output
        val goVersionOutput = goVersionProcess.inputStream.bufferedReader().readText()
        val goVersionErrorOutput = goVersionProcess.errorStream.bufferedReader().readText()

        goVersionProcess.waitFor()

        if (goVersionProcess.exitValue() != 0) {
            println("Error accessing Go. Please check the installation.")
            println("Error output: $goVersionErrorOutput")
            return@doLast
        }

        // Install gomobile
        try {
            println("Installing gomobile...")
            val gomobileInstallCommand = ProcessBuilder(goBinPath, "install", "golang.org/x/mobile/cmd/gomobile@latest")
            val gomobileProcess = gomobileInstallCommand.start()

            gomobileProcess.waitFor()

            if (gomobileProcess.exitValue() == 0) {
                println("gomobile installed successfully!")
                // Save gomobile path to a global variable
                val gomobilePath = "$goBinDir/gomobile"
                project.extra["gomobilePath"] = gomobilePath
                println("Saved gomobile path: $gomobilePath")
            } else {
                println("Error installing gomobile. Please check Go and try again.")
            }
        } catch (e: Exception) {
            println("Error trying to install gomobile: ${e.message}")
        }
    }
}

tasks.register<Copy>("copySharedLibs") {
    description = "Copies the generated .so files to the libs folder"
    group = "build"

    from("${buildDir}/intermediates/go-libs/armeabi-v7a/")
    into("${projectDir}/src/main/jniLibs/armeabi-v7a/")

    from("${buildDir}/intermediates/go-libs/arm64-v8a/")
    into("${projectDir}/src/main/jniLibs/arm64-v8a/")
}

tasks.named("preBuild") {
    dependsOn("installGo", "installGoMobile", "generateGoSharedLibs")
}
dependencies {


    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
}
