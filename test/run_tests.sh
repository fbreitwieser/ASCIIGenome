#!/bin/bash

# These tests are far from comprehensive since they don't check the interactive session
## MEMO: use `less -R -S` to view colours on terminal with less

# Setup: Path to jar and data
# ===========================
stvExe=~/Dropbox/Public/ASCIIGenome.jar ## Path to jar 
#cd /Users/berald01/svn_git/ASCIIGenome/branches/toggle_print/test_data ## Path to test data
mkdir -p test_tmp
cd test_tmp

# Get and prepare chr7.fa file, if not already available
if [ ! -e chr7.fa ]
    then
    wget http://hgdownload.cse.ucsc.edu/goldenPath/hg19/chromosomes/chr7.fa.gz &&
    gunzip chr7.fa.gz
    samtools faidx chr7.fa
fi

# Start tests
# ===========

JAVA_OPTIONS=-Djava.net.useSystemProxies=true -Xmx500m

set -x
echo "CAN LOAD BAM FILES"
echo java $(JAVA_OPTIONS) -jar $stvExe -r chr7:5598650-5601530 ds051.actb.bam ear045.oxBS.actb.bam -ni
java $(JAVA_OPTIONS) -jar $stvExe -r chr7:5598650-5601530 ds051.actb.bam ear045.oxBS.actb.bam -ni
java ${JAVA_OPTIONS} -jar $stvExe -r chr7:5598650-5601530 -fa chr7.fa ds051.actb.bam ear045.oxBS.actb.bam -ni 
java ${JAVA_OPTIONS} -jar $stvExe -rpm -r chr7:5598650-5601530 ds051.actb.bam ear045.oxBS.actb.bam -ni 
java ${JAVA_OPTIONS} -jar $stvExe ds051.actb.bam -r chr7:5566860 -x 'mapq 10 -f 16' -ni 

echo "CAN SHOW BS DATA"
java ${JAVA_OPTIONS} -jar $stvExe -r chr7:5600000-5600179 -fa chr7.fa ds051.actb.bam ear045.oxBS.actb.bam -x 'mapq 10 && BSseq' -ni 

echo "HANDLE NO READS IN INPUT"
java ${JAVA_OPTIONS} -jar $stvExe ds051.actb.bam -r chr7:5566860 -x ' -f 16 && -F 16' -ni # Note space bebween quote and -f

echo "BED FILES"
java ${JAVA_OPTIONS} -jar $stvExe refSeq.hg19.short.bed -ni
java ${JAVA_OPTIONS} -jar $stvExe refSeq.hg19.short.sort.bed.gz -ni

echo "FROM URL"
java ${JAVA_OPTIONS} -jar $stvExe http://hgdownload.cse.ucsc.edu/goldenPath/hg19/encodeDCC/wgEncodeSydhTfbs/wgEncodeSydhTfbsGm12878P300bStdPk.narrowPeak.gz -ni

echo "TABIX FILES"
java ${JAVA_OPTIONS} -jar $stvExe test.bedGraph.gz -ni

echo "BIGWIG FROM URL"
java ${JAVA_OPTIONS} -jar $stvExe -r chr7:5494331-5505851 http://hgdownload.cse.ucsc.edu/goldenPath/hg19/encodeDCC/wgEncodeSydhTfbs/wgEncodeSydhTfbsGm12878Nrf1IggmusSig.bigWig -ni

echo "TDF"
java ${JAVA_OPTIONS} -jar $stvExe -r chr7:1-2149128 hg18_var_sample.wig.v2.1.30.tdf -ni

echo "GTF TABIX"
java ${JAVA_OPTIONS} -jar $stvExe -r chr7:1-2149128 hg19.gencode_genes_v19.gtf.gz -ni

echo "FIND REGEX"
java ${JAVA_OPTIONS} -jar $stvExe -r chr7:5772765-5772967 -fa chr7.fa -x 'seqRegex (?i)ac..tg' -ni 

echo "GRACEFULLY HANDLE INVALID INPUT"
java ${JAVA_OPTIONS} -jar $stvExe refSeq.hg19.short.bed -x 'foo' -ni
java ${JAVA_OPTIONS} -jar $stvExe refSeq.hg19.short.bed -x 'ylim 0 10 *' -ni
java ${JAVA_OPTIONS} -jar $stvExe foo.bed -ni

echo -e "\n\nDONE\n\n"

echo -e "PRINT HELP"
java ${JAVA_OPTIONS} -jar $stvExe -h &&

cd ..
rm -rf test_tmp

exit
