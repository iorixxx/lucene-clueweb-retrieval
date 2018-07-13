#!/usr/bin/perl -w

# 13.07.2018 Friday
# Modified to report nDCG@20 instead of nDCG@30

# Who:    Virgil Pavlu
# What:   Evaluation statAP
# When:   09/07
# version 3 computes also PC,RP, NDCG and estimates from the sample the variance of the AP estimate.

use LWP::Simple;

# Usage
if (@ARGV <2) {
  die "\n Usage: staAP_MQ_eval.pl [-q][-n/-m][-f][-t] prel_5column_file   trec_file[+] > output\n        -q = display results per topic\n        -n = evaluate only NEU-exclusive judged topics\n        -m = evaluate only alternating judged topics\n        -f = use only NEU judged docs on alternating topics\n        -t = print only the measure numbers (per topic) \n\n" ;
  }

$force_mixed_eval =1;
$results_per_query = 0;
$print_long=1;
$querytype=-1;
$cutoff_10 = 10; $cutoff_20 = 20; $cutoff_50 = 50; $cutoff_100 = 100;
$NDCG_threshold = 20;

$prel_file = shift;
if ($prel_file eq "-q") {
	$results_per_query=1;
	$prel_file = shift;	
}

if ($prel_file eq "-n") {
	$querytype=1;
	$prel_file = shift;	
}
if ($prel_file eq "-m") {
	$querytype=2;
	$prel_file = shift;	
}
if ($prel_file eq "-f") {
	$force_mixed_eval=0;
	$prel_file = shift;	
}

if ($prel_file eq "-t") {
	$print_long=0;
	$prel_file = shift;	
}
			
$old_topic =-1;
my $old_judge =-1;
my ($ignored, $exp, $r, $judgeid, $timestamp);
#sub vipf($xx);
##########################################################################################
# Process prel file.
print STDERR " Processing the prel file...";
open(PREL, $prel_file) or die "Failed to open $prel_file: $!\n\n";

{
local $/ = undef;			# Reads grab the whole file.
@data = split(/\s+/, <PREL>);		# Data array has all values from the
}					# file consecutively.

close(PREL) or die "Couldn't close $prel_file: $!\n\n";

%qrel_relevance = ();
%qrel_iprob =();
%qrel_judged_NEU =();
%qrel_judged_UMASS =();
%no_sampled=();
%no_sampled_relev=();
my %qrel_judge =();


################################
#for prel with 7 fields
#while (($topic, $doc_id, $rel, $judgeid, $timestamp, $method, $iprob) = splice(@data,0,7)) {
#    	if($old_topic==$topic && $old_judge>$judgeid){ # the new judge is the right one
#		delete $qrel_judge{$topic};
#		delete $qrel_relevance{$topic};
#		delete $qrel_iprob{$topic};    	
#		$qrel_judged_UMASS{$topic}=0;
#		$qrel_judged_NEU{$topic}=0;
#		$old_judge=$judgeid;
#    	}
#    	if($old_topic==$topic && $old_judge<$judgeid){ # the old judge is the right one
#		next;
#    	}
#    	
#    	if($old_topic!=$topic) {  # new topic	
#		$qrel_judged_UMASS{$topic}=0;
#		$qrel_judged_NEU{$topic}=0;
#		$old_topic = $topic;
#		$old_judge=$judgeid;	
#	}		
#		
#
#	if($iprob eq "NA") {next;}# ignore docs with NA incl probab
#	if($iprob <=0 || $iprob >1 ) {next;}# ignore docs with inclprob<=0 or >1
#		 
#	if($method==0 || $method==2) {$qrel_judged_UMASS{$topic}+=1;}
#	if($method==1 || $method==2 || $method==4) {$qrel_judged_NEU{$topic}+=1;}
#		 	
#	if($method==0 && $force_mixed_eval==0) {next;} 	 	
#			 
#	if($rel<=0)  {next;}	
#	$rel =1; #all relevant docs are the same relevance
#	
#	if(exists($qrel_judge{$topic}->{$doc_id}) && $qrel_judge{$topic}->{$doc_id}<$judgeid) {;} else {
#	
#		$qrel_judge{$topic}->{$doc_id}= $judgeid;
#		$qrel_relevance{$topic}->{$doc_id} = $rel;
#		$qrel_iprob{$topic}->{$doc_id} = $iprob;
#	}
#}


################################
##for prel with 5 fields
while (($topic, $doc_id, $rel, $method, $iprob) = splice(@data,0,5)) {
    	if($rel==-999) {next;}
    	if($old_topic!=$topic) {	
		$qrel_judged_UMASS{$topic}=0;
		$qrel_judged_NEU{$topic}=0;
		$old_topic = $topic;	
		$no_sampled_relev{$topic}=0;
		$no_sampled{$topic} =0;
		$no_sampled_veryrelev{$topic}=0;
	}		
	if($iprob eq "NA") {next;}# ignore docs with NA incl probab
	if($iprob <0 || $iprob >1 ) {next;}# ignore docs with inclprob<=0 or >1
#	if($iprob<0.01) {$iprob=0.01;}
	if($rel<=-999 ) {next;}# ignore docs not sampled
		 
	if($method==0 || $method==2) {$qrel_judged_UMASS{$topic}+=1;}
	if($method==1 || $method==2 || $method==4) {$qrel_judged_NEU{$topic}+=1;}
		 	
	if($method==0 && $force_mixed_eval==0) {next;} #ignore UMASS docs if so asked	 	
	if($method==0 && $force_mixed_eval==1) {$iprob=1;} #set the collision iprob=1
	if($method==2 && $force_mixed_eval==1) {$iprob=1;} #set the collision iprob=1

		 
#	if($rel<=0)  {next;}
	if($rel==1) {$no_sampled_relev{$topic} += 1;} 	
	if($rel>1) {$no_sampled_relev{$topic} += 1;$no_sampled_veryrelev{$topic} += 1;} #all relevant docs are the same relevance

	$qrel_relevance{$topic}->{$doc_id} = $rel;
	$qrel_iprob{$topic}->{$doc_id} = $iprob;
	$no_sampled{$topic} += 1;
	
}
$no_topics_judged = scalar keys %qrel_judged_NEU;
print STDERR "done. Topics judged: $no_topics_judged\n";



##########################################################################################
# process trec files
foreach $trec_file (@ARGV) {
	print STDERR "\n\n ======================== input: $trec_file ===========================\n";
  	open(TREC, $trec_file) or die "Failed to open $trec_file: $!\n\n";
#	print STDERR "(judged topics read from input file: ";
 	%docscore = ();
 	%sys = ();
 	%syslength =();
 	$old_topic=-1;
     $topiccounter =0;
	while(<TREC>){
		$line = $_;
		@dline = split(/\s+/, $line);
		($topic, $ignored, $doc_id, $r, $s, $exp) = splice(@dline,0,6);		
		if(!exists($qrel_judged_NEU{$topic})) {next;}
 		if($old_topic!=$topic) {		
			$syslength{$topic} =0;
			$old_topic = $topic;
#			if ($topiccounter++ % 10==0) {print STDERR "$topiccounter ";}
		}	
		$syslength{$topic} =  $syslength{$topic}+1;
    		$docscore{$topic}->{$doc_id} = $s;
	}
	close (TREC); print STDERR ")";

##########################################	
	$no_valid_topics=0;
	$sum_AP=0; $sum_RP=0; $sum_PC_10=0;$sum_PC_20=0;$sum_PC_50=0;$sum_PC_100=0;
	$sum_var_statAP=0;
	$sum_NDCG_10=0;$sum_NDCG_20=0;$sum_NDCG_50=0;$sum_NDCG_100=0;

	$sum_RQ_estimated =0;
  	foreach $topic (sort {$a<=>$b} keys %qrel_judged_NEU){
  	# ignore topics judged but not searched by this run	
  		if(!exists($syslength{$topic})) {
  		 	next;	
  		}
  		if($results_per_query==1){
  			if($print_long==1) {print "\n	topic=$topic  ";}
  			else {print "$topic  ";}
  		}   	
  	#ignore topics judged UMASS exclusively
  		if($qrel_judged_NEU{$topic}==0) { 
  			if($results_per_query==1) {
  		 		print "UMASS\n";
  			}
  		 	next;	
  		}
  	#ignore topics judged NEU exclusively (if so required)
  		if($qrel_judged_UMASS{$topic}==0) { 
  			
  			if($results_per_query==1) {
  				print "NEU ";  
  		 		if($querytype==2 || $print_long==1) {print "\n";}
  			}	
  			if($querytype==2) {next;}
  		}
  	#ignore topics judged alternate (if so required)
  		if($qrel_judged_UMASS{$topic}>0 && $qrel_judged_NEU{$topic}>0 ){ 
  			if($results_per_query==1) {
  		# 		print "MIXED ";
  		 		if( $querytype==1 || $print_long==1) {print "\n";}
  			}
  		 	if( $querytype==1) {next;}
  		 		
  		}
 		
	  	$topicscores = $docscore{$topic};
	  	$topic_relevance = $qrel_relevance{$topic};
	  	
	
  	###################################
  	#estimation RQ
		$RQ_estimated =0;
		foreach $doc_id (keys  %$topic_relevance){
			$iprob= $qrel_iprob{$topic}->{$doc_id};
	#		if($iprob<0.02) {$iprob=0.02;}
			if($qrel_relevance{$topic}->{$doc_id}>0){
				$RQ_estimated += 1 / $iprob;	 
			}
		}

		#order by scores	
		@sys=();
		$rrank=0;	  
		%doc_inlist =();
		@iarray_iprob=();
		@iarray_prec=();
  		foreach $doc_id (sort {$topicscores->{$b}<=>$topicscores->{$a}} keys %$topicscores) {
			$rrank++;
			$sys[$rrank]=$doc_id;	
			$doc_inlist{$doc_id}=$rrank;
		} 
	
	
	############################### compute NDCG normalization constant
	$NDCG_10_normalization =0;$NDCG_20_normalization =0;$NDCG_50_normalization =0;$NDCG_100_normalization =0;
	$rank=0;
	foreach $doc_id (sort {$topic_relevance->{$b}<=>$topic_relevance->{$a}} keys %$topic_relevance) {
		$rel = $qrel_relevance{$topic}->{$doc_id};
		if($rel<=0) {last;}
		$rank++;
		if($rank<=10) {$NDCG_10_normalization += NDCG_rating($rel)/NDCG_discount($rank);}
		if($rank<=20) {$NDCG_20_normalization += NDCG_rating($rel)/NDCG_discount($rank);}
		if($rank<=50) {$NDCG_50_normalization += NDCG_rating($rel)/NDCG_discount($rank);}
		if($rank<=100) {$NDCG_100_normalization += NDCG_rating($rel)/NDCG_discount($rank);}
			else {last;}
	}	
#	for ($r=1;$r<=$no_sampled_veryrelev{$topic} ;$r++){
#		if($r>$NDCG_threshold) {next;}
#		$NDCG_normalization += NDCG_rating(2)/NDCG_discount($r);
#  	}
#	for ($r=$no_sampled_veryrelev{$topic}+1; $r<=$no_sampled_relev{$topic}-$no_sampled_veryrelev{$topic} ;$r++){
#		if($r>$NDCG_threshold) {next;}
#		$NDCG_normalization += NDCG_rating(1)/NDCG_discount($r);
#	}
#  	
	######################################################
	# evaluation statAP
		$RQ_estimated_rounded = round($RQ_estimated);
		$Prec_cutoff_10 = 0;$Prec_cutoff_20 = 0;$Prec_cutoff_50 = 0;$Prec_cutoff_100 = 0;
		$RPrec =-1;
		$sum_prec = 0;
		$sum_final_up=0;
		$sum_final_down=$RQ_estimated;
		$NDCG_10_sum = 0;$NDCG_20_sum = 0;$NDCG_50_sum = 0;$NDCG_100_sum = 0;
		$NDCG_10=0;$NDCG_20=0;$NDCG_50=0;$NDCG_100=0;
		$var_statAP = -1;
		$no_sampled_inlist=0;
		$no_sampled_relev_inlist=0;
		@iarray_iprob=();
 		@iarray_prec=();
 		@iarray_rank=();
 
		if ($sum_final_down>0) {
	 		for ($r=1;$r<=$syslength{$topic};$r++){
 				$doc_id= $sys[$r];
 				$rel=0;
 				if (exists($topic_relevance->{$doc_id})) {
 					#	print STDERR "$doc_id \n";
 					$no_sampled_inlist++;
		 			$rel =$qrel_relevance{$topic}->{$doc_id};
		 			if($rel>0){
		 				$no_sampled_relev_inlist++;
 						$iprob = $qrel_iprob{$topic}->{$doc_id};
 						$prec = (1+$sum_prec)/$r;
 						$sum_final_up += $prec/$iprob;
 						$iarray_iprob[$no_sampled_relev_inlist]=$iprob;
 						$iarray_prec[$no_sampled_relev_inlist]=$prec;
 						$iarray_rank[$no_sampled_relev_inlist]=1/$r;
 						$sum_prec += 1/$iprob;
 					}					
 				}
 				####### NDCG
 				if($r<=10) {$NDCG_10_sum += NDCG_rating($rel) / NDCG_discount($r);}
 				if($r<=20) {$NDCG_20_sum += NDCG_rating($rel) / NDCG_discount($r);}
 				if($r<=50) {$NDCG_50_sum += NDCG_rating($rel) / NDCG_discount($r);}
 				if($r<=100) {$NDCG_100_sum += NDCG_rating($rel) / NDCG_discount($r);}
 				######evaluation Precision_at_cutoff $cutoff
				if ($r==$cutoff_10){ $Prec_cutoff_10 = $sum_prec/$r;}
				if ($r==$cutoff_20){ $Prec_cutoff_20 = $sum_prec/$r;}
				if ($r==$cutoff_50){ $Prec_cutoff_50 = $sum_prec/$r;}
				if ($r==$cutoff_100){ $Prec_cutoff_100 = $sum_prec/$r;}
				#######evaluation RPrecision
				if ($r==$RQ_estimated_rounded){ $RPrec = $sum_prec/$r;}		
			}
			$NDCG_10 = $NDCG_10_sum / $NDCG_10_normalization;
			$NDCG_20 = $NDCG_20_sum / $NDCG_20_normalization;
			$NDCG_50 = $NDCG_50_sum / $NDCG_50_normalization;
			$NDCG_100 = $NDCG_100_sum / $NDCG_100_normalization;
			if ($Prec_cutoff_10<0) {$Prec_cutoff_10 = $sum_prec/$cutoff_10};
			if ($Prec_cutoff_20<0) {$Prec_cutoff_20 = $sum_prec/$cutoff_20};
			if ($Prec_cutoff_50<0) {$Prec_cutoff_50 = $sum_prec/$cutoff_50};
			if ($Prec_cutoff_100<0) {$Prec_cutoff_100 = $sum_prec/$cutoff_100};
			if( $RPrec<0) {$RPrec = $sum_prec/$RQ_estimated};
		
			$statAP = $sum_final_up/$sum_final_down;
##################################################################### variance estimation
#for the rel ones in the list shift the prec by $statAP
		for($c=1;$c<=$no_sampled_relev_inlist;$c++){
			$iarray_prec[$c] -= $statAP;
			$iarray_prec[$c] = $iarray_prec[$c] / $iarray_iprob[$c];
		}		
# record the relevants not found in the list 		
		$count_rel = $no_sampled_relev_inlist;
		foreach $docID (keys  %$topic_relevance){
		 	$rel =$qrel_relevance{$topic}->{$docID};
		 	if($rel>0){
		 #		print "\n $docID   $qrel_iprob{$topic}->{$docID}   ";
		 		if(exists($doc_inlist{$docID})){
		 #			print "rank=".$doc_inlist{$docID}."   "; 
		 		}
		 		else{	
		 #			print "-1";	
					$count_rel++;
 					$iprob = $qrel_iprob{$topic}->{$docID};
 					$iarray_iprob[$count_rel]=$iprob;
 					$iarray_prec[$count_rel]=-$statAP / $iprob;	
 					$iarray_rank[$count_rel]=0;		
		 		}
			}
		}
#estimate variance
		$sum1=0; $sum2=0; $sum3=0; $sum4=0;

		for ($i=$count_rel;$i>$no_sampled_relev_inlist;$i--){
			$ip= $iarray_iprob[$i]; 
			$yy= $iarray_prec[$i];
		#	print "$i  ip=$ip  yy/ip=$yy\n ";
			$sum1+= (1-$ip)* $yy*$yy;
			for($j=$i-1;$j>0;$j--){
			#	if( $ip != $iarray_iprob[$j]){
					$sum2 += 2* $yy * $iarray_prec[$j]; 
			#	}
			}
		}

		for ($i=$no_sampled_relev_inlist;$i>0;$i--){
			$ip= $iarray_iprob[$i]; 
			$yy= $iarray_prec[$i];
		#	print "$i  ip=$ip  yy/ip=$yy\n ";
			$sum1+= (1-$ip)* $yy*$yy;
			for($j=$i-1;$j>0;$j--){
			#	if( $ip != $iarray_iprob[$j]){
					$sum2 += 2* $yy * $iarray_prec[$j]; 
			#	}
			}
			
			$sum3 += $sum4*$sum4 * (1-$ip) /$ip/$ip;
			$sum4 += $iarray_rank[$i] / $ip;		
		}


#		$var_statAP = ($sum1 - $sum2/($no_sampled{$topic} -1)  + $sum3) / $RQ_estimated /$RQ_estimated;
		$var_statAP = ($sum1 - $sum2/($no_sampled{$topic} -1) +$sum3) / $RQ_estimated /$RQ_estimated /($no_sampled_inlist+0.1)*$no_sampled{$topic};
		#$var_statAP = ($sum1) / $RQ_estimated /$RQ_estimated;
		if($var_statAP>1) {$var_statAP=1;}
#################################################################################### 
			$no_valid_topics+=1;
			$sum_AP +=$statAP;$sum_var_statAP += $var_statAP;
			$sum_RP +=$RPrec;
			$sum_PC_10 +=$Prec_cutoff_10;$sum_PC_20 +=$Prec_cutoff_20;$sum_PC_50 +=$Prec_cutoff_50;$sum_PC_100 +=$Prec_cutoff_100;
			$sum_NDCG_10 +=$NDCG_10;$sum_NDCG_20 +=$NDCG_20;$sum_NDCG_50 +=$NDCG_50;$sum_NDCG_100 +=$NDCG_100;
			$sum_RQ_estimated += $RQ_estimated;
			$statAPprint = vipf($statAP);
			

		
		}
		else{ 	
				if($print_long==1) {
					$statAPprint="NA--no relevant docs";
					$RPrec_print="NA--no relevant docs";
					$Prec_cutoff_print=0;
				}
				else {
					$statAPprint= -999;
					$RPrec_print= -999;
					$Prec_cutoff_print=0;
				}
		}
		$NDCG_10_print = vipf($NDCG_10);
		$NDCG_20_print = vipf($NDCG_20);	
		$NDCG_50_print = vipf($NDCG_50);
		$NDCG_100_print = vipf($NDCG_100);
			
		$Prec_cutoff_10_print = vipf($Prec_cutoff_10);
		$Prec_cutoff_20_print = vipf($Prec_cutoff_20);
		$Prec_cutoff_50_print = vipf($Prec_cutoff_50);
		$Prec_cutoff_100_print = vipf($Prec_cutoff_100);
		$RPrec_print = vipf($RPrec);			
	######################################################
	# printing		
		$RQ_estimated_print= vipf($RQ_estimated);
		$no_sampled = $qrel_judged_NEU{$topic} ; 
		if($force_mixed_eval>0){$no_sampled += $qrel_judged_UMASS{$topic};}
		$no_rel_topic= $no_sampled_relev{$topic};
		if($results_per_query==1){
			if($print_long==1){
				print "	Relevant=$RQ_estimated_print  sampled=$no_sampled{$topic} sampled_relev=$no_rel_topic\n"; 
				print "	sampled_in_list=$no_sampled_inlist  sampled_relev_in_list=$no_sampled_relev_inlist\n"; 
				print "	AP=$statAPprint  R-prec=$RPrec_print\n";  
				print "	Prec_at_$cutoff_10=$Prec_cutoff_10_print Prec_at_$cutoff_20=$Prec_cutoff_20_print Prec_at_$cutoff_50=$Prec_cutoff_50_print Prec_at_$cutoff_50=$Prec_cutoff_100_print\n";  
				print " 	nDCG_10=$NDCG_10_print nDCG_20=$NDCG_20_print nDCG_50=$NDCG_50_print nDCG_100=$NDCG_100_print\n";
				print "	varAP=$var_statAP\n";				
			}
			
			
			
			else {print "$statAPprint  $RQ_estimated_print  $RPrec_print  $Prec_cutoff_10_print $Prec_cutoff_20_print $Prec_cutoff_50_print $Prec_cutoff_100_print $NDCG_10_print $NDCG_20_print $NDCG_50_print $NDCG_100_print  $var_statAP  $no_sampled_inlist $no_sampled_relev_inlist \n";}	
		}	
	}
	
	$MAP = 	$sum_AP / $no_valid_topics;
	$MRP = 	$sum_RP / $no_valid_topics;
	$MPC_10 = 	$sum_PC_10 / $no_valid_topics;
	$MPC_20 = 	$sum_PC_20 / $no_valid_topics;
	$MPC_50 = 	$sum_PC_50 / $no_valid_topics;
	$MPC_100 = 	$sum_PC_100 / $no_valid_topics;
	$M_var_statAP = $sum_var_statAP/$no_valid_topics/$no_valid_topics; 
	$MNDCG_10 = $sum_NDCG_10/ $no_valid_topics;
	$MNDCG_20 = $sum_NDCG_20/ $no_valid_topics;
	$MNDCG_50 = $sum_NDCG_50/ $no_valid_topics;
	$MNDCG_100 = $sum_NDCG_100/ $no_valid_topics;
#	print "\n\n $sum_RP"; print "\n\n $sum_AP"; print "\n\n $sum_PC"; die;
	if($print_long==1){
		print "\n valid_topics=$no_valid_topics";    
		print "\n statMAP_on_valid_topics=$MAP    VAR_MAP=$M_var_statAP ";
		print "\n statMRP_on_valid_topics=$MRP";  
		print "\n statMPC_$cutoff_10 _on_valid_topics=$MPC_10 ";
		print "\n statMPC_$cutoff_20 _on_valid_topics=$MPC_20 ";
		print "\n statMPC_$cutoff_50 _on_valid_topics=$MPC_50 ";
		print "\n statMPC_$cutoff_100 _on_valid_topics=$MPC_100 ";
		print "\n statMNDCG_10_on_valid_topics=$MNDCG_10 ";
		print "\n statMNDCG_20_on_valid_topics=$MNDCG_20 ";
		print "\n statMNDCG_50_on_valid_topics=$MNDCG_50 ";
		print "\n statMNDCG_100_on_valid_topics=$MNDCG_100 \n\n";
	}
	else{
		print  "\n $no_valid_topics   $MAP   $sum_RQ_estimated $MRP  $MPC_10 $MPC_20 $MPC_50 $MPC_100 $MNDCG_10 $MNDCG_20 $MNDCG_50 $MNDCG_100  $M_var_statAP";    
#		print STDERR "\n statMAP_on_valid_topics=$MAP ( VAR_MAP=$M_var_statAP )";
#		print STDERR "\n statMRP_on_valid_topics=$MRP";  
#		print STDERR "\n statMPC_$cutoff _on_valid_topics=$MPC ";
#		print STDERR "\n statMNDCG_on_valid_topics=$MNDCG \n\n";
	}	
}










sub vipf{
    my $x = shift;
    return sprintf("%.3f",$x) ;  
} 
sub round {
    my($number) = shift;
    return int($number + .5);
}
sub log2 {
	my $n = shift;
	return log($n)/log(2);
    }
sub NDCG_discount{
    my($rank) = shift;
    return log2($rank+1);
}
sub NDCG_rating{
    my($rel) = shift;
    $ret=((1<<$rel) - 1);
    return $ret;
}
  