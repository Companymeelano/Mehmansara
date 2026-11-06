; Inno Setup 6 script for the MEHRA Windows installer (Mehra-setup.exe)
; Built by .github/workflows/windows-ci.yml — env vars: MEHRA_PUBLISH (dir containing Mehra.exe), MEHRA_OUT (output dir)
#define MyAppName "مهرآ"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "فرهنگان اهواز"
#define MyAppExeName "Mehra.exe"
#ifndef MyPublishDir
#define MyPublishDir "publish"
#endif
#ifndef MyOutputDir
#define MyOutputDir "out\exe"
#endif

[Setup]
AppId={{7C4E9A21-3B58-4F6D-9E1A-2D8B5F0C41A7}}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL=https://github.com/Companymeelano/Mehmansara
DefaultDirName={localappdata}\Mehra
DefaultGroupName={#MyAppName}
PrivilegesRequired=lowest
OutputDir={#MyOutputDir}
OutputBaseFileName=Mehra-setup
SetupIconFile=desktop\Mehra.Desktop\Assets\mehra.ico
UninstallDisplayIcon={app}\{#MyAppExeName}
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
DisableProgramGroupPage=yes

[Languages]
Name: "persian"; MessagesFile: "compiler:Languages\Persian.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "{#MyPublishDir}\Mehra.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "out\exe\README.txt"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{group}\راهنمای سریع (README)"; Filename: "{app}\README.txt"
Name: "{userdesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "ایجاد آیکن روی دسکتاپ"; GroupDescription: "اضافه‌های به گروه برنامه"

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "اجرای مهرآ"; Flags: nowait postinstall skipifsilent
