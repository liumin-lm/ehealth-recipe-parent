for /f "delims=" %%i in ('git rev-parse --abbrev-ref HEAD') do set nowBranch=%%i
git fetch
git checkout %nowBranch%
git pull
git checkout devfixed
git pull
git merge origin/%nowBranch% --no-ff
git push -u origin devfixed
git checkout %nowBranch%

pause
