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
		set key inside reverse Left top right horizontal box
		set key font 'Helvetica Bold,14'
		set key width 0
		set key samplen 2
		set key spacing 2
		set encoding iso_8859_1
		set title \"Taux de violation du SLA\" offset 0,-0.8,0
		set terminal pdf enhanced color font 'Helvetica Bold,18'
        set output \"big_sla_report.pdf\"
        set datafile separator \",\"
        set xlabel \"Seuil maximum d'utilisation\"
        set ylabel \"Taux de violation du SLA (\%)\"
        set yrange [0:*]
        set format x \"%g %%\"
        set format y \"%g %%\"
        set xtics 70,5,95
        set xrange [67:98]
		set grid
        plot \"./$FILE1\" using 3:6 with lp ps 1 lw 2 ti \"Glouton\" ,\
        \"./$FILE1\" using 3:9 with lp ps 1 lw 2 ti \"HPSD\" ,\
        \"./$FILE1\" using 3:12 with lp ps 1 lw 2 ti \"HPPD\" ,\
        \"./$FILE1\" using 3:15 with lp ps 1 lw 2 ti \"Sans stockage\" " | gnuplot
