UKMaApp
=======
How to Build the UK MaApp
How To Build and Run the Application
In order to build the application, Android Studio needs to be installed on the computer. In order to install Android Studio, you need to have a Java Runtime Environment (JRE). All of these will be included in the provided CD.
Follow these steps in order to build the application:

1.	Install the JRE to the computer with the default settings. The file name to install is: “jre-7-windows-i586”

2.	Install the JDK to the computer with the default settings. The file name to install is: “jdk-8u25-windows-i586”

3.	Install Android Studio on the computer with the default settings. The file name to install is: “android-studio-bundle-135.1339820-windows”

4.	Open Android Studio. Let it run for a bit and wait for it to request whether you want to create a new project or import a project.

5.	Import the project files included in the folder labeled “Project Development Files > UKMaApp”. The project will open in Android Studio and you will see the files in the gradle.

6.	At the top, there will be a button towards the center that looks like a play button. 
Press this and the project should build and run.

7.	You will be asked to pick an emulator to use for the project. Pick an emulator from the list.

8.	After an emulator has been chosen, the emulator window will appear with the MaApp running. Note: the emulators usually run slowly on computers. Give it time and it should run.

Items Needed To Run the App
The only items need to run the application is Android Studio. Android Studio comes with a built in emulator which has a wide range of devices to test on and a wide range of SDK versions.
You may also use a phone attached to the computer through a USB port. This will appear on the list of devices when you debug in Android Studio.

Environmental Considerations
The items needed for the environment is the JDK (Supplied) and SDK (Supplied). The platform used was Windows 7 to run Android Studio.

Setting up an Emulator
Steps to setup a new emulator if one has not been installed or an android device is not available for testing:

1.)	Click on the AVD Manager that can be found on the quick access toolbar or through Tools > Android> AVD Manager.

2.)	Given time, the AVD Manager will load. All created devices will be visible on the first tab, Android Virtual Devices. Name the AVD Device any name - Anything will do here. The other setting should be matched as below:

AVD NAME:

DEVICE: NEXUS 5 (4.0", 480x800 hdpi)

TARGET: Google APIs (Google Inc.) - API Level 19

CPU/ABI: - unable to change

KEYBOARD: click the box for Hardware keyboard present

SKIN: Skin with dynamic hardware controls

FRONT CAMERA: None

Back CAMERA: None

MEMORY OPTIONS: RAM: 512 VM HEAP: 32

INTERNAL STORAGE: 200 MB

SD CARD: SIZE: 20 MB

FILE - not clicked

EMBEDDED OPTIONS: Snapbox and Use Host GPU both not checked


3.)	Click OK.

4.)	The Device has now been created, so close the AVD. When the program is built and ran, chose the emulator that was created by the name chosen in step 2.
