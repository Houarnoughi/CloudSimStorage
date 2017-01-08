#!/bin/sh

if [ $# -lt 1 ]; then
  echo "Usage :"$0" <stats_csv>"
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
		set title \"Co�t total de placement des VM\" offset 0,-0.8,0
		set terminal pdf enhanced color font 'Helvetica Bold,18'
        set output \"cost_report.pdf\"
        set datafile separator \",\"
        set xlabel \"Nombre de VM\"
        set ylabel \"Co�t normalis�\"
        set yrange [0:8]
        set xrange [-1:9]
		# set format y \"%g %%\"
		set grid
		set key inside top left horizontal box
		set key font 'Helvetica Bold,14'
		set key width -1
		set key samplen 2
		set key spacing 2
		set grid
        plot \"./$FILE1\" using 3:xticlabels(2) with lp ps 1 lw 2 ti \"Glouton\" ,\
        \"./$FILE1\" using 5:xticlabels(2) with lp ps 1 lw 2 ti \"HPSD\" ,\
        \"./$FILE1\" using 7:xticlabels(2) with lp ps 1 lw 2 ti \"HPPD\" ,\
        \"./$FILE1\" using 9:xticlabels(2) with lp ps 1 lw 2 ti \"Excate\" ,\
        \"./$FILE1\" using 11:xticlabels(2) with lp ps 1 lw 2 ti \"Sans stockage\" " | gnuplot