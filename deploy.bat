@REM ./gradlew build
copy .\forge-1.20.1\build\libs\forge-1.20.1-0.1.0.jar "X:\Games\mc_modpacks\Instances\Zaruba Reborn\mods\"
scp -i "C:\Users\danyl\OneDrive\Desktop\uf_connect\key" .\forge-1.20.1\build\libs\forge-1.20.1-0.1.0.jar root@unrealfrontiers.xyz:~/unrealfrontiers/gameservers/zaruba-servers/zaruba-lobby-1/mods/
