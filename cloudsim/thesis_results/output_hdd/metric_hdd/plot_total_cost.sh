#!/bin/sh

if [ $# -lt 4 ]; then
  echo "Usage :"$0" <BFMC_csv> <Greedy_csv> <HPSD_csv> <HPPM_csv>"
  exit 1
fi

FILE1=$1
FILE2=$2
FILE3=$3
FILE4=$4

if [ ! -e $FILE ]; then
  echo "Error: $1 does not exist"
  exit 1
fi

# Define bounds
#~ t_max=$(awk -F ";" '{ print $1 }' $FILE | sort -n | sed -n '$p')
#~ p_max=$(( $(awk -F ";" '{ print $2 }' $FILE | sort -n | sed -n '$p') + 5 ))
#~ p_min=$(( $(awk -F ";" '{ print $2 }' $FILE | sort -n | sed -n '1p') - 5 ))

# The output file name
OUT=$(echo $FILE | awk -F "." '{print $1}').eps

# Plot the graph for seq operations
echo "
		clear
		reset
		unset key
		set encoding iso_8859_1
		set title \"Coût total\" offset 0,-0.8,0
		set terminal pdf enhanced color font 'Helvetica Bold,18'
        set output \"total_cost.pdf\"
        set datafile separator \";\"
        set xlabel \"Nombre de VM\"
        set ylabel \"Coût (\$)\"
        set yrange [0:*]
        set xrange [-1:6]
		# set format y \"%g %%\"
		set grid
		set key inside top left vertical box
		set key font 'Helvetica Bold,15'
		set key width 2
		#set key spacing 0.6
        plot \"./$FILE1\" using 13:xticlabels(2) with lp ps 1 lw 1 ti \"Exhaustive\" ,\
        \"./$FILE2\" using 13:xticlabels(2) with lp ps 1 lw 1 ti \"Greedy\" ,\
        \"./$FILE3\" using 13:xticlabels(2) with lp ps 1 lw 1 ti \"HPSD\" ,\
        \"./$FILE4\" using 13:xticlabels(2) with lp ps 1 lw 1 ti \"HPPM\" " | gnuplot
