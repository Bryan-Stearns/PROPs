# R-File to plot the data from the Chein and Morrison experiment

library("data.table", lib.loc="~/R/x86_64-pc-linux-gnu-library/3.4")

plot_wmspan <- function(ylims, dat1, dat2, leg) {
  x11(width=5,height=5)
  par(lwd=2)
  plot(dat1,type="b",pch=1,ylab="WM Span (items)",xlab="Training session",ylim=ylims)
  #png(paste(dirpath, "fig_human_chein.png", sep=""), width=5,height=5, units="in",res=300)
  #par(lwd=2, mar=c(3,3,1,1), mgp=c(2,0.5,0), cex.lab=1.2)
  if (!missing(dat2)) {
    lines(dat2,type="b",pch=2)
    #legend(1,6,legend=c("Data","Model"),pch=c(1,2),lty=1)
    legend(1,ylims[2],legend=leg,pch=c(1,2),lty=1,bty="n")
  }
}

plot_stroop_intfr <- function(ylims, dat1, dat1err) {
  library(Hmisc)
  x11(width=4,height=5)
  par(lwd=2)
  errbar(2:3,dat1[1,],dat1[1,]+dat1err[1,],dat1[1,]-dat1err[1,],ylim=ylims,type="b",xlab="",xaxt="n",xlim=c(1,4),ylab="Stroop Interference (ms)")
  errbar(2:3,dat1[2,],dat1[2,]+dat1err[2,],dat1[2,]-dat1err[2,],add=T,type="b",pch=2)
  axis(1,at=2:3,labels=c("Data Pre","Data Post"))
  legend(1,25,legend=c("No training","WM training"),pch=1:2,lty=1,bty="n")
}

plot_stroop_prepost <- function(ylims, ys1a,ys1b,ys2a,ys2b, labs) {
  x11(width=7,height=5)
  #png(paste(dirpath, "fig_human_chein.png", sep=""), width=7,height=5, units="in",sres=300)
  par(lwd=2)
  plot(2:3,ys1a,ylim=ylims,type="b",xlab="",xaxt="n",xlim=c(1,6),ylab="Interference (ms)",main="Stroop")
  lines(2:3,ys1b,type="b",pch=2)
  lines(4:5,ys2a,type="b")
  lines(4:5,ys2b,type="b",pch=2)
  axis(1,at=2:5,labels=labs)
  #text(c(2.5,5),c(115,90),labels=c("Reactive\nmodel","Proactive\nmodel"))
  #legend(1,160,legend=c("No training","WM training"),pch=1:2,lty=1)
  legend(1,.25*ylims[2],legend=c("No training","WM training"),pch=1:2,lty=1,bty="n")
}

######## SET PARAMS HERE ########

PLOT_L1 <- TRUE           # The case where chunks were pre-included for memory reference tracing (false) or not (true)
PLOT_L2 <- TRUE           # The case where chunking was turned on for instruction combo evaluation results (subsumes L1 results)
PLOT_L3 <- FALSE           # The case where chunking was turned on for the complete evaluation result (learns away instruction use)

t <- "1"
sample <- "_s4"
PLOT_SOURCE <- "PROPS"

#===============================#

graphname <- ifelse(PLOT_SOURCE=="PRIMS", "", 
                    paste("_l", ifelse(PLOT_L1, "1", ""), ifelse(PLOT_L2, "2", ""), ifelse(PLOT_L3, "3", ""), 
                          "_t", t, sample, sep=""))

modelpath <- ifelse(PLOT_SOURCE=="PRIMS",
                    "/home/bryan/Actransfer/supplemental/Actransfer distribution/CheinMorrison/original/",
                    "/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/cheinmorrison/results/")

# Human VCWM data:
dada <- c( 0, 5.9, 6.7, 0.8, 3.6, 9.2, 6.6, 13.9, 11.2, 13.9, 11.8, 9.1, 12.5, 12.8, 12.8, 16.8, 15.5, 13.9, 20.8, 22.5)
dada <- (dada/100+1)*4.33 
# Human Stroop data
exp.stroop <- rbind(c(120.3,91.5),c(127.8,58.7))
exp.stroop.se <- rbind(c(135.9,103.1),c(148.0,78.8))-exp.stroop
# Human Stroop data, simplified (?):
exp.sstroop <- rbind(c(120,95),c(120,60))

# Plot original Actransfer data
#sdat <- read.table("/home/bryan/Actransfer/supplemental/Actransfer distribution/CheinMorrison/original/stroopChein.txt")
#names(sdat) <- c("task","condition","block","day","trial","type","correct","RT")
#sres <- with(sdat[sdat$block>0,],tapply(RT,list(day,condition,type),mean))

# Rehearsal model Stroop data:
sdat <- read.table(paste(modelpath,"stroopChein",graphname,"_X.dat",sep=""), fill=TRUE, 
                  col.names = c("task","condition","block","day","trial","type","correct","RT","DC","FT","fetches","prepares"))
sres <- with(sdat,tapply(RT,list(day,condition,type),mean))  # sres = RT in [{1,21}, {CONTROL,EXP}, {CONGRUENT,INCONGRUENT}]

# Plots the human and rehearsal model Stroop data (omitting block 1 from model data)
sres
(sres[,1,2]-sres[,1,1])*1000
(sres[,2,2]-sres[,2,1])*1000
plot_stroop_prepost(c(0,160), exp.sstroop[1,], exp.sstroop[2,],
                    (sres[,1,2]-sres[,1,1])*1000, (sres[,2,2]-sres[,2,1])*1000, 
                    c("Data Pre","Data Post","Model Pre","Model Post"))

# Non-rehearsal model Stroop data:
sdatNR <- read.table(paste(modelpath,"stroopCheinNR",graphname,"_X.dat",sep=""), fill=TRUE, 
                    col.names = c("task","condition","block","day","trial","type","correct","RT","DC","FT","fetches","prepares"))
sresNR <- with(sdatNR,tapply(RT,list(day,condition,type),mean))



# Rehearsal model VCWM data:
vdat <- read.table(paste(modelpath,"WMChein",graphname,"_X.dat",sep=""), fill=TRUE, 
                   col.names = c("day","span","correct","DC","FT","fetches","prepares"))
vdat.m <- with(vdat[vdat$correct==1,],tapply(span,day,mean))
#vdat.m <- with(vdat,tapply(span,day,mean))

# Non-rehearsal model VCWM data:
vdatNR <- read.table(paste(modelpath,"WMCheinNR",graphname,"_X.dat",sep=""), fill=TRUE, 
                   col.names = c("day","span","correct","DC","FT","fetches","prepares"))
vdatNR.m <- with(vdatNR[vdatNR$correct==1,],tapply(span,day,mean))
#vdat.m <- with(vdat,tapply(span,day,mean))


# Plot just the human VCWM data (as it is in the paper)
plot_wmspan(c(3,6), dada)

# Plot just the human Stroop data (as it is in the paper)
plot_stroop_intfr(c(0,160), exp.stroop, exp.stroop.se)

# Plot the human and model VCWM data side by side
plot_wmspan(c(1,7), dada, vdat.m, c("Data","Model"))

# Plot the rehearsal vs non-rehearsal models side by side
plot_wmspan(c(1,7), vdat.m, vdatNR.m, c("Reactive model","Proactive model"))



#sdat$subject <- as.factor(as.integer((as.numeric(rownames(sdat))-1)/768))
#sres <- with(sdat,tapply(RT,list(day,condition,type,subject),mean))
sres[,2,]-sres[,1,]

# Plot interference (INCONGRUENT-CONGRUENT) in non-rehearsal vs rehearsal Stroop models
plot_stroop_prepost(c(0,200), (sresNR[,1,2]-sresNR[,1,1])*1000, (sresNR[,2,2]-sresNR[,2,1])*1000, 
                    (sres[,1,2]-sres[,1,1])*1000, (sres[,2,2]-sres[,2,1])*1000,
                    c("Reactive\nPre","Reactive\nPost","Proactive\nPre","Proactive\nPost"))
