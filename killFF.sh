t=`ps -ef | grep firefox | awk '{print($2)}'` ; kill -9 $t
t=`ps -ef | grep firefox | grep defunct | awk '{print($3)}'` ; kill -9 $t
