if [ $# -ne 1 ]; then
  echo "Error: Need to pass in the sport"
  exit 1
fi
./caesars.sh      $1 true
./betrivers_d.sh  $1 false ; killFF.sh
./fanduel_d.sh    $1 false ; killFF.sh
./draftkings_d.sh $1 false ; killFF.sh
./betmgm_d.sh     $1 false ; killFF.sh
./espn_d.sh       $1 false ; killFF.sh


