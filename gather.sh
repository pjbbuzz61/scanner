if [ $# -ne 1 ]; then
  echo "Error: Need to pass in the sport"
  exit 1
fi
./betrivers.sh  $1 true
./betmgm.sh     $1 false
./espn.sh       $1 false
./fanduel.sh    $1 false
./caesars.sh    $1 false
./draftkings.sh $1 false

