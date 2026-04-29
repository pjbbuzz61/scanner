t=`ps -ef | grep firefox | grep pat | egrep -v grep | awk '{print($2)}'` ; kill -9 $t
sleep 2
t=`ps -ef | grep firefox | grep pat | grep defunct | awk '{print($3)}'`  ; kill -9 $t


