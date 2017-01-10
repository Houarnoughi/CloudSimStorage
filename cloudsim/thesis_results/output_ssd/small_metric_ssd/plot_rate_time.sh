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
		set title \"Temps d'ex�cution des algorithmes d'optimisation\" offset 0,-0.8,0
		set terminal pdf enhanced color font 'Helvetica Bold,18'
        set output \"small_time_report_miops.pdf\"
        set datafile separator \",\"
        set xlabel \"Nombre de VM\"
        set ylabel \"Temps normalis�\"
        #~ set yrange [0:*]
        set xrange [-1:10]
		# set format y \"%g %%\"
		set key inside reverse Left top left vertical box
		set key font 'Helvetica Bold,14'
		set key width 0.5
		set key samplen 2
		set key spacing 1
		set grid
		set log y 2
        plot \"./$FILE1\" using 4:xticlabels(2) with lp ps 1 lw 2 ti \"Glouton\" ,\
        \"./$FILE1\" using 6:xticlabels(2) with lp ps 1 lw 2 ti \"HPSD\" ,\
        \"./$FILE1\" using 8:xticlabels(2) with lp ps 1 lw 2 ti \"HPPD\" ,\
        \"./$FILE1\" using 10:xticlabels(2) with lp ps 1 lw 2 ti \"Excate\" ,\
        \"./$FILE1\" using 12:xticlabels(2) with lp ps 1 lw 2 ti \"Sans stockage\" " | gnuplot
