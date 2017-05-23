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
		set key outside Left reverse top right horizontal box
		set key font 'Helvetica Bold,12'
		set key width -0.5
		set key samplen 2
		set key spacing 1
		set encoding iso_8859_1
		set title \"Temps d'ex�cution\" offset 0,-0.8,0
		set terminal pdf enhanced color font 'Helvetica Bold,18'
        set output \"big_time_report_miops.pdf\"
        set datafile separator \",\"
        set xlabel \"Seuil maximum d'utilisation\" offset 0, 1
        set ylabel \"Temps d'ex�cution (seconde)\"
        set yrange [0:*]
        set format x \"%g %%\"
        set xtics offset 0,graph 0.05
        set xtics 70,5,95
        set xrange [67:98]
		set grid
        plot \"./$FILE1\" using 3:5 with lp ps 1 lw 2 ti \"Approche gloutonne\" ,\
        \"./$FILE1\" using 3:8 with lp ps 1 lw 2 ti \"HPSD\" ,\
        \"./$FILE1\" using 3:11 with lp ps 1 lw 2 ti \"HPPM\" ,\
        \"./$FILE1\" using 3:14 with lp ps 1 lw 2 ti \"Beloglazov et al [26]\" " | gnuplot
