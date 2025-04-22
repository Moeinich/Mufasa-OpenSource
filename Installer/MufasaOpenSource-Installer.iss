[Setup]
AppName=Mufasa (OpenSource)
AppPublisher=JustDavyy
AppPublisherURL=https://github.com/JustDavyy/Mufasa-OpenSource
SetupIconFile=mufasa.ico
AppVersion=1.00
DefaultDirName={localappdata}\MufasaOpenSource
DisableDirPage=yes
DisableProgramGroupPage=yes
OutputBaseFilename=Mufasa OpenSource Installer
Compression=lzma
SolidCompression=yes
UninstallDisplayIcon={app}\mufasa.ico

[Files]
Source: "mufasa.7z"; DestDir: "{app}"; Flags: ignoreversion
Source: "7z.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "mufasa.ico"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{userdesktop}\Mufasa"; Filename: "javaw.exe"; Parameters: "-jar ""{app}\Mufasa.jar"""; WorkingDir: "{app}"; IconFilename: "{app}\mufasa.ico"

[UninstallDelete]
Type: filesandordirs; Name: "{app}"
Type: dirifempty; Name: "{app}"

[Code]
procedure CurStepChanged(CurStep: TSetupStep);
var
  ZipFile, ExtractTo, SevenZip, Params: string;
  ResultCode: Integer;
begin
  if CurStep = ssPostInstall then
  begin
    ExtractTo := ExpandConstant('{app}');
    ZipFile := ExtractTo + '\mufasa.7z';
    SevenZip := ExtractTo + '\7z.exe';

    Log('Unzipping in final app directory...');
    Log('7z.exe path: ' + SevenZip);
    Log('Zip path: ' + ZipFile);
    Log('Target path: ' + ExtractTo);

    Params := 'x "' + ZipFile + '" -o"' + ExtractTo + '" -y';
    if Exec(SevenZip, Params, '', SW_HIDE, ewWaitUntilTerminated, ResultCode) then
    begin
      Log('7z.exe returned exit code: ' + IntToStr(ResultCode));
      if ResultCode = 0 then
      begin
        DeleteFile(ZipFile); // Clean up the zip
        DeleteFile(SevenZip); // Clean up 7z.exe too
      end
      else
        MsgBox('Extraction failed with code ' + IntToStr(ResultCode), mbError, MB_OK);
    end
    else
      MsgBox('Failed to execute 7z.exe', mbError, MB_OK);
  end;
end;

// REMARK: Uninstalling will remove the desktop shortcut and all contents from your Mufasa folder.
