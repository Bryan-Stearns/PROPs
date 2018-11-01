# R-file to plot the data from the Elio experiment and model.

plot_elio_data <- function(auto, cog, f_pch) {
  # This function assume the global workspace variables used at the time it is called below
  
  f_graphname <- paste("l",ifelse(PLOT_L1,"1",""),"2",ifelse(auto, "3",""), ifelse(PLOT_SPREADING, "s", ""), ifelse(PLOT_MANUAL, "m", ""), ifelse(cog, "c", ""), ifelse(PLOT_EPSETS, "e", ""), "_t", T, "_s", samples, sep="")
  f_inpath <- paste("/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/elio/results/verbose_elio_props_", f_graphname,"_X",".dat", sep="")
  f_data <- read.table(f_inpath)
  f_data <- data.frame(f_data[1:11], f_data[4] - 0.05*f_data[6])  # Action Latencies = RT - DC_time
  f_data <- data.frame(f_data[1:12], 1.0*(f_data[12]+f_data[9]+0.05*f_data[8]))  # ST = Actions + LTtime + counted_DCs
  names(f_data) <- c("task","trial","line","RT","answer", "DC1s", "chunks", "DCs", "LTtime", "LTcount", "fails", "ACT", "ST")
  
  f_data$type <- ifelse((f_data$task %in% c("PROCEDURE-A","PROCEDURE-C") & (f_data$line %in% c(1,2,4))) | (f_data$task %in% c("PROCEDURE-B","PROCEDURE-D") & (f_data$line < 4)), "component","integrative")
  f_data$block <- trunc((f_data$trial+4)/5)
  f_data.m <- with(f_data,tapply(DCs,list(task,block,type),mean))
  if (PLOT_ST) {
    f_data.m <- with(f_data,tapply(ST,list(task,block,type),mean))
  }
  
  lines((1:10*5),f_data.m[1,,taskInd],type="b",pch=f_pch,lty=1,ylim=c(yfloor,yscale),xlim=c(0,xscale),main=title,col="black")
  
  train_points <- f_data.m[1,,1]
  mse <- mean((t - train_points)^2)
  print(train_points)
  print(paste(ifelse(auto,"Auto,","Control,"),ifelse(cog,"Learned","Known"), " MSE: ",mse,sep = ""))
}

# Plot of the model data
PLOT_ST <- TRUE
PLOT_TASK <- "Component"
PLOT_HUMAN <- TRUE
PLOT_ACTR <- FALSE
PLOT_PROPv1 <- FALSE
PLOT_PROPv2 <- FALSE
PLOT_PROPv240 <- FALSE
PLOT_PROPdev <- TRUE

PLOT_L1 <- TRUE           # The case where chunks were pre-included for memory reference tracing (false) or not (true)
PLOT_L2 <- TRUE           # The case where chunking was turned on for instruction combo evaluation results (subsumes L1 results)
PLOT_L3 <- FALSE           # The case where chunking was turned on for the complete evaluation result (learns away instruction use)
PLOT_SPREADING <- TRUE    # The case where instructions are recalled according to activation and condition spread (plotted on v240)
PLOT_MANUAL <- FALSE      # The case where a manual sequence is used (true) or not (false)
PLOT_LC <- FALSE          # The case where chunks that trigger condition spreading are to be learned (true) or pre-loaded (false)
PLOT_EPSETS <- TRUE       # The case where fetch-guiding sets are used (true) or not (false)

DC_MODE <- ""    # "" = 50ms, "40" = 40ms
T <- "10"
samples <- "8"

graphname = paste("l", ifelse(PLOT_L3 && !PLOT_L2 && !PLOT_L1, 
                              paste("3only", ifelse(PLOT_LC, "c", ""), sep=""),
                              paste(ifelse(PLOT_L1, "1", ""), ifelse(PLOT_L2, "2", ""), ifelse(PLOT_L3, "3", ""), 
                                    ifelse(PLOT_SPREADING, "s", ""), ifelse(PLOT_MANUAL, "m", ""), ifelse(PLOT_LC, "c", ""), ifelse(PLOT_EPSETS, "e", ""), sep="")), 
                  "_t", T, "_s", samples, sep="")

if (PLOT_ACTR) {
  prim_filepath <- "/home/bryan/Actransfer/supplemental/Actransfer distribution/Elio/MyResults/elio-out8_X.dat"
  prim_data <- read.table(prim_filepath)
  
  prim_data <- data.frame(prim_data[1:10],lapply(prim_data[6],(function(x) 50.0*x/1000.0)))
  names(prim_data) <- c("task","trial","line","ST","answer", "DCs", "chunks", "sample", "retrievals", "avgrt", "DCT")
    
  prim_data$type <- ifelse((prim_data$task %in% c("procedure-a","PROCEDURE-C") & (prim_data$line %in% c(1,2,4))) | (prim_data$task %in% c("PROCEDURE-B","PROCEDURE-D") & (prim_data$line < 4)), "component","integrative")
  prim_data$block <- trunc((prim_data$trial+4)/5)
  prim_data.m <- with(prim_data,tapply(DCs,list(task,block,type),mean))
  if (PLOT_ST) {
    prim_data.m <- with(prim_data,tapply(ST,list(task,block,type),mean))
  }
}
if (PLOT_PROPv1) {
  prop1_filepath <- paste("/home/bryan/Documents/Research/PRIMsDuplications/Elio/PROPs_v1.0/verbose_elio_props_nofinal_t",T,"l50_X4pr.dat",sep="")
  prop1_data <- read.table(prop1_filepath)
  
  prop1_data <- data.frame(prop1_data[1:13], prop1_data[11]+prop1_data[13])
  names(prop1_data) <- c("task","trial","line","RT","answer", "DC1s", "chunks", "sample", "DCs", "ST1", "ST2", "fails", "latencies", "ST")
  
  prop1_data$type <- ifelse((prop1_data$task %in% c("procedure-a","PROCEDURE-C") & (prop1_data$line %in% c(1,2,4))) | (prop1_data$task %in% c("PROCEDURE-B","PROCEDURE-D") & (prop1_data$line < 4)), "component","integrative")
  prop1_data$block <- trunc((prop1_data$trial+4)/5)
  prop1_data.m <- with(prop1_data,tapply(DCs,list(task,block,type),mean))
  if (PLOT_ST) {
    prop1_data.m <- with(prop1_data,tapply(ST,list(task,block,type),mean))
  }
}
if (PLOT_PROPv2) {
 prop2_filepath <- paste("/home/bryan/Documents/Research/PRIMsDuplications/Elio/verbose_elio_props_", graphname,"_X1pr",".dat", sep="")
 prop2_data <- read.table(prop2_filepath)

 prop2_data <- data.frame(prop2_data[1:13], prop2_data[11]+prop2_data[13])
 names(prop2_data) <- c("task","trial","line","RT","answer", "DC1s", "chunks", "sample", "DCs", "ST1", "ST2", "fails", "latencies", "ST")

prop2_data$type <- ifelse((prop2_data$task %in% c("procedure-a","PROCEDURE-C") & (prop2_data$line %in% c(1,2,4))) | (prop2_data$task %in% c("PROCEDURE-B","PROCEDURE-D") & (prop2_data$line < 4)), "component","integrative")
 prop2_data$block <- trunc((prop2_data$trial+4)/5)
 prop2_data.m <- with(prop2_data,tapply(DCs,list(task,block,type),mean))
 if (PLOT_ST) {
   prop2_data.m <- with(prop2_data,tapply(ST,list(task,block,type),mean))
 }
}
if (PLOT_PROPv240) {
  prop240_filepath <- paste("/home/bryan/Documents/Research/PRIMsDuplications/Elio/verbose_elio_props_", ifelse(PLOT_SPREADING, "sc_", ""), graphname,"_X1pr",DC_MODE,".dat", sep="")
  prop240_data <- read.table(prop240_filepath)
  
  prop240_data <- data.frame(prop240_data[1:13], prop240_data[11]+prop240_data[13])
  names(prop240_data) <- c("task","trial","line","RT","answer", "DC1s", "chunks", "sample", "DCs", "ST1", "ST2", "fails", "latencies", "ST")

  prop240_data$type <- ifelse((prop240_data$task %in% c("procedure-a","PROCEDURE-C") & (prop240_data$line %in% c(1,2,4))) | (prop240_data$task %in% c("PROCEDURE-B","PROCEDURE-D") & (prop240_data$line < 4)), "component","integrative")
  prop240_data$block <- trunc((prop240_data$trial+4)/5)
  prop240_data.m <- with(prop240_data,tapply(DCs,list(task,block,type),mean))
  if (PLOT_ST) {
    prop240_data.m <- with(prop240_data,tapply(ST,list(task,block,type),mean))
  }
}

xscale <- 50
ylabel <- ifelse(PLOT_ST, "Time (s)", "Decision Cycles")
yscale <- ifelse(PLOT_ST, 15, 240)

yfloor <- 0
legendY <- 0.99
savename = paste("fig_elio_",ifelse(PLOT_ACTR, "m",""),ifelse(PLOT_PROPv1, "1",""),ifelse(PLOT_PROPv2, "2",""),ifelse(PLOT_PROPv240, "240",""),ifelse(PLOT_PROPdev, "3",""),ifelse(PLOT_SPREADING,"_","_"),sep="")
prefix <- ifelse(PLOT_ST, paste("STpr",ifelse(PLOT_PROPv240,DC_MODE,""),sep=""), "DC")

title <- ifelse(PLOT_L1,
                ifelse(PLOT_L3, "Full Learning", "Partial Learning"),
                ifelse(PLOT_L3, "Levels 2-3", "Level 2")
              )
title <- paste("PROPs, ", title, sep="")
maintxt <- ifelse(PLOT_ACTR,"Actransfer KNOWN-AUTO", paste("",ifelse(PLOT_PROPv240,"KNOWN-",""),ifelse(PLOT_L3,"AUTO","DELIBERATE"),sep=""))
legendText <- c("Human")
legendPch <- c(4)
if (PLOT_ACTR) {legendText <- c(legendText,"Actransfer"); legendPch<-c(legendPch, 3)}
if (PLOT_PROPv1) {legendText <- c(legendText,"PROP1"); legendPch<-c(legendPch, 6)}
if (PLOT_PROPv2) {legendText <- c(legendText,ifelse(PLOT_PROPv240 && !PLOT_SPREADING,"KNOWN 50ms","AUTO")); legendPch<-c(legendPch, 1)}
if (PLOT_PROPv240) {legendText <- c(legendText,ifelse(PLOT_SPREADING, "PROP2 Auto+Free", paste("KNOWN ",DC_MODE,"ms",sep=""))); legendPch<-c(legendPch, 5)}

taskInd <- 1 # 1==Component, 2==Integrative
if (PLOT_TASK == "Component") {t <- 2.28+8.59*(1:10)^(-.98)} else {t <- 1.49+6.43*(1:10)^(-1.19); taskInd <- 2;}

# PLOT
png(paste("/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/elio/results/", savename, prefix, PLOT_TASK, "_", graphname, ".png", sep=""),
    width=3.75,height=4.5, units="in",res=300)
par(lwd=2,mar=c(3,3,2,1))
plot((1:10*5),t,type="b",pch=4,lty=1,ylim=c(0,15),xlim=c(0,55),main=maintxt,cex.main=1.0,col=ifelse(PLOT_HUMAN,"gray37","white"))
if (PLOT_ACTR) {lines((1:10*5),prim_data.m[1,,taskInd],type="b",pch=3,lty=1,ylim=c(yfloor,yscale),xlim=c(0,xscale),main=title,col="black")}
if (PLOT_PROPv1) {lines((1:10*5),prop1_data.m[1,,taskInd],type="b",pch=6,lty=1,ylim=c(yfloor,yscale),xlim=c(0,xscale),main=title,col="black")}
if (PLOT_PROPv2) {lines((1:10*5),prop2_data.m[1,,taskInd],type="b",pch=1,lty=1,ylim=c(yfloor,yscale),xlim=c(0,xscale),main=title,col="black")}
if (PLOT_PROPv240) {lines((1:10*5),prop240_data.m[1,,taskInd],type="b",pch=5,lty=1,ylim=c(yfloor,yscale),xlim=c(0,xscale),main=title,col="black")}

if (PLOT_PROPdev) {
  #plot_elio_data(PLOT_L3, TRUE, 2); legendText <- c(legendText, paste("LEARNED",sep="")); legendPch <- c(legendPch, 2);
  plot_elio_data(PLOT_L3, FALSE, 1); legendText <- c(legendText, paste("KNOWN",ifelse(PLOT_EPSETS," proposals 50ms",""),sep="")); legendPch <- c(legendPch, 1);
}

title(xlab="Trial", ylab=ylabel, line=2)

legend(ifelse(PLOT_SPREADING,5,15),legendY*yscale,legend=legendText,lty=c(1,1,1,1,1),pch=legendPch, col=c("gray37","black","black","black","black"), pt.cex=1, cex=1.0)
dev.off() # Finalize image

if (PLOT_ACTR) {
  train_points <- prim_data.m[1,2:10,1]
  mse <- mean((t[2:10] - train_points)^2)
  print(train_points)
  print(paste("PRIMs MSE: ",mse,sep = ""))
}
# if (PLOT_PROPv2) {
#   train_points <- prop2_data.m[1,2:10,1]
#   mse <- mean((t[2:10] - train_points)^2)
#   print(train_points)
#   print(paste("PROPs MSE: ",mse,sep = ""))
# }
if (PLOT_PROPv240) {
  train_points <- prop240_data.m[1,2:10,1]
  mse <- mean((t[2:10] - train_points)^2)
  print(train_points)
  print(paste("PROPs ",DC_MODE,"ms MSE: ",mse,sep = ""))
}


