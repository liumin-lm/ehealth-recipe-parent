for /f "delims=" %%i in ('git rev-parse --abbrev-ref HEAD') do set nowBranch=%%i
git fetch
git checkout %nowBranch%
git pull
git checkout master
git pull
git merge origin/%nowBranch% --no-ff
git push -u origin master
git checkout %nowBranch%

pause
