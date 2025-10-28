if [ $# -ne 1 ]; then
  echo "Error: Need to pass in the sport"
  exit 1
fi
./betmgm.sh $1 true ; ./killFF.sh
./espn.sh $1 false ; ./killFF.sh
./fanduel.sh $1 false ; ./killFF.sh

