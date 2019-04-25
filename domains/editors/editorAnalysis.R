# R file to make graphs of the model results of the editor model
# And also to plot the data

transfer <- function(M1,Mn,Tn) { (M1-Tn)/(M1-Mn) }
pwid <- 4
pht <- 3

# Plot of the model data
PLOT_SOURCE <- "PROP3"    # "PROP2", "PROP3", "PRIMS", "HUMAN"
PLOT_VAR <- "time"          # "time", "DC", "RT", "retrievals", "avg_rt", "DCtime"
PLOT_EXTRAS <- TRUE

PLOT_L1 <- FALSE           # The case where chunks were pre-included for memory reference tracing (false) or not (true)
PLOT_L2 <- TRUE           # The case where chunking was turned on for instruction combo evaluation results (subsumes L1 results)
PLOT_L3 <- FALSE           # The case where chunking was turned on for the complete evaluation result (learns away instruction use)

dirpath <- "/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/editors/results/known-fetch/"
T <- c("48")
sample = "_s2"

READ_VERBOSE <- TRUE

# This is the aggregate data from the experiment
dat.ededemacs <- c(115, 54, 44, 42, 43, 28)
dat.edtedtemacs <- c(214, 87, 55, 49, 43, 28)
dat.ededtemacs <- c(115,54,63,44,41,26)
dat.edtedemacs <- c(214, 87, 46, 37, 41, 26)
dat.emacsemacsemacs <- c(77, 37, 29, 23, 23, 21)

if (PLOT_SOURCE == "HUMAN") {
  
  # Plot the data
  
  #x11(width=pwid,height=pht)
  #png(paste(dirpath, "fig_human_editors_all.png", sep=""), width=pwid,height=pht, units="in",res=300)
  #par(lwd=2)
  #plot(1:6,dat.ededemacs,ylim=c(0,100),xlab="Day",ylab="Seconds/correct operation",type="b",pch=2)
  #abline(v=2.5,lty=3,lwd=1)
  #abline(v=4.5,lty=3,lwd=1)
  #lines(dat.edtedtemacs,type="b",pch=4)
  #lines(dat.ededtemacs,type="b",pch=4,lty=2)
  #lines(dat.edtedemacs,type="b",pch=2,lty=2)
  #lines(dat.emacsemacsemacs,type="b",pch=1)
  #legend(3,99,legend=c("edt-edt-emacs","ed-ed-emacs","edt-ed-emacs","ed-edt-emacs","emacs-emacs-emacs"),lty=c(1,1,3,3,1),pch=c(4,2,2,4,1),bg="white", pt.cex=1, cex=0.8)
  #dev.off()
  
  # Data plot, but now split into three panels
  
  # ED-ED-EMACS, EDT-EDT-EMACS, EMACS-EMACS-EMACS
  # png(paste(dirpath, "fig_human_editors_plain.png", sep=""), width=pwid,height=pht, units="in",res=300)
  # #par(mfrow=c(2,1))
  # par(lwd=2, mar=c(3,3,1,1), mgp=c(2,0.5,0), cex.lab=1.2)
  # plot(1:6,dat.ededemacs,ylim=c(0,100),xlab="Day",ylab="Seconds/correct operation",type="b",pch=2, main="Human")
  # abline(v=2.5,lty=3,lwd=1)
  # abline(v=4.5,lty=3,lwd=1)
  # lines(dat.edtedtemacs,type="b",pch=4)
  # #lines(dat.ededtemacs,type="b",pch=4,lty=2)
  # #lines(dat.edtedemacs,type="b",pch=2,lty=2)
  # lines(dat.emacsemacsemacs,type="b",pch=1)
  # legend(2.9,99,legend=c("edt-edt-emacs","ed-ed-emacs","emacs-emacs-emacs"),lty=1,pch=c(4,2,1),bg="white", pt.cex=1, cex=0.9)
  # dev.off()
  # 
  # EDT-ED-EMACS, ED-ED-EMACS
  # #x11(width=pwid,height=pht)
  # png(paste(dirpath, "fig_human_editors_edted.png", sep=""), width=pwid,height=pht, units="in",res=300)
  # par(lwd=2, mar=c(3,3,1,1), mgp=c(2,0.5,0), cex.lab=1.2)
  # plot(1:6,dat.ededemacs,ylim=c(0,100),type="b",pch=2,xlab="Day",ylab="Seconds/correct operation", main="Human")
  # abline(v=2.5,lty=3,lwd=1)
  # abline(v=4.5,lty=3,lwd=1)
  # lines(dat.edtedemacs,type="b",pch=2,lty=2)
  # legend(3,99,legend=c("edt-ed-emacs","ed-ed-emacs"),pch=2,lty=c(3,1),bg="white", pt.cex=1, cex=1)
  # dev.off()
  
  # ED-EDT-EMACS, EDT-EDT-EMACS
  png(paste(dirpath, "fig_human_editors_ededt.png", sep=""), width=pwid,height=pht, units="in",res=300)
  par(lwd=2, mar=c(3,3,1,1), mgp=c(2,0.5,0), cex.lab=1.2)
  plot(1:6,dat.edtedtemacs,ylim=c(0,100),type="b",pch=4,xlab="Day",ylab="Seconds/correct operation", main="Human")
  abline(v=2.5,lty=3,lwd=1)
  abline(v=4.5,lty=3,lwd=1)
  lines(dat.ededtemacs,type="b",pch=4,lty=2)
  legend(3.3,y_range[2],legend=c("edt-edt-emacs","ed-edt-emacs"),pch=3,lty=c(1,3),bg="white", pt.cex=1, cex=1)
  dev.off()
  
  # Calculate Global transfer
  dat.ed2edt <- transfer(214,55,63)
  dat.edt2ed <- transfer(115,44,46)
  dat.line2emacs <- transfer(77,23,(43+43+41+41)/4)
  
} else{
  
  if (PLOT_SOURCE == "PRIMS") {
    T <- c("")
  }
  
  if (PLOT_SOURCE == "PROP3" && PLOT_L1) {
    OPR <- c("180","185")
    TMP <- c("08","10","12","14","16")
  }
  else {
    OPR <- c("")
    TMP <- c("")
  }
    
  for (t in T) {
    for (opr in OPR) {
      for (tmp in TMP) {
    
        graphname = ifelse(PLOT_SOURCE=="PRIMS", "", 
                           paste("_l", ifelse(PLOT_L1, "1", ""), ifelse(PLOT_L2, "2", ""), ifelse(PLOT_L3, "3", ""), 
                                 ifelse(PLOT_SOURCE=="PROP2", "m",""),
                                "_t", t, 
                                ifelse(PLOT_SOURCE=="PROP3" && PLOT_L1, paste("_lr02_dr775_opr", opr, "_tmp", tmp, sep=""), ""),
                                sample, sep=""))
        
        filepath <- ifelse(PLOT_SOURCE=="PRIMS", paste(dirpath,"editor_out2_X.dat",sep=""),
                                  paste(dirpath,ifelse(PLOT_SOURCE=="PROP2","PROP2/",""),ifelse(READ_VERBOSE,"verbose_",""),ifelse(PLOT_SOURCE=="PROP2","editors_props","editors_prop3"),graphname,ifelse(READ_VERBOSE,"_X",""),".dat", sep="")
                          )
        
        # Read in the model results
        dat <- read.table(filepath)
        if (PLOT_SOURCE=="PRIMS") {
          names(dat) <- c("condition","day","editor","trial","type","ll","mt","time", "DC","learned","runs","retrievals","avg_rt")
          dat$DCtime <- dat$DC*0.05
        } else if (PLOT_SOURCE=="PROP2") {
          dat <- data.frame(dat[1:13], dat[9] + dat[11])
          dat <- data.frame(dat[1:14],lapply(dat[10],(function(x) x*0.05)))
          names(dat) <- c("condition","day","editor","trial","type","ll","mt","RT","latency","DC","ST","retrievals","fails","time","DCtime")
        }
        else {
          if (READ_VERBOSE) {
            names(dat) <- c("condition","day","editor","trial","type","ll","mt","RT","latency","DC","LTM_time","retrievals","countOp")
            dat$DCtime <- dat$DC*0.05
            dat$time <- dat$DCtime + dat$latency + dat$LTM_time
          } else {
            names(dat) <- c("condition","day","editor","trial","type","ll","mt","RT","latency")
          }
        }
        dat.m <- with(dat,tapply(switch(PLOT_VAR, "time"=time,"DC"=DC,"RT"=RT,"retrievals"=retrievals,"avg_rt"=avg_rt,"DCtime"=DCtime)
                                  ,list(condition,day),mean))
        
        # Plot the model
        if (PLOT_VAR=="time") {
          y_range <- c(0,100)
          y_label <- "Seconds/correct operation"
        } else if (PLOT_VAR=="DCtime") {
          y_range <- c(0,100)
          y_label <- "Seconds/correct operation"
        } else if (PLOT_VAR=="DC") {
          y_range <- c(0,300)
          y_label <- "Decisions/correct operation"
        } else if (PLOT_VAR=="retrievals") {
          y_range <- c(0,100)
          y_label <- "Retrievals/correct operation"
        } else if (PLOT_VAR=="RT") {
          y_range <- c(0,100)
          y_label <- "Seconds/correct operation"
        } else {
          y_range <- c(0.0,1.25)
          y_label <- "Avg Retrieval Time (s)"
        }
        
        legend_y <- 1.0*y_range[2]
        legend_x <- 3.3
        legend_scale <- 1
        savename <- paste(dirpath, ifelse(PLOT_SOURCE=="PRIMS", "fig_prims_editors", "fig_prop2_editors"), switch(PLOT_VAR,"time"="RT","DC"="DC","retrievals"="LT","avg_rt"="ART","DCtime"="DCT"), graphname, sep="")
        
        #x11(width=pwid,height=pht)
        #par(lwd=2, mar=c(3,3,1,1), mgp=c(2,0.5,0), cex.lab=1.2)
        #plot(1:6,dat.m["ED-ED-EMACS",],xlab="Day",ylab=y_label,type="b",pch=2,ylim=y_range)
        #lines(dat.m["EDT-EDT-EMACS",],type="b",pch=4)
        #lines(dat.m["ED-EDT-EMACS",],type="b",pch=4,lty=2)
        #lines(dat.m["EDT-ED-EMACS",],type="b",pch=2,lty=2)
        #lines(dat.m["EMACS-EMACS-EMACS",],type="b")
        #legend(3.5,legend_y,legend=c("edt-edt-emacs","ed-ed-emacs","edt-ed-emacs","ed-edt-emacs","emacs-emacs-emacs"),lty=c(1,1,3,3,1),pch=c(4,2,2,4,1),bg="white")
        
        # Same, but now split into three panels
        maintxt <- paste("Editors - ", PLOT_SOURCE, ifelse(PLOT_VAR=="DCtime", ", No retrievals",""),sep="")
        
        # #x11(width=6,height=6)
        # png(paste(savename,"_plain.png",sep=""), width=pwid,height=pht, units="in",res=300)
        # par(lwd=2, mar=c(3,3,1,1), mgp=c(2,0.5,0), cex.lab=1.2)
        # plot(1:6,dat.m["ED-ED-EMACS",],ylim=y_range,xlab="Day",ylab=y_label,type="b",pch=2)
        # abline(v=2.5,lty=3,lwd=1)
        # abline(v=4.5,lty=3,lwd=1)
        # lines(dat.m["EDT-EDT-EMACS",],type="b",pch=4)
        # lines(dat.m["EMACS-EMACS-EMACS",],type="b")
        # legend(legend_x,legend_y,legend=c("edt-edt-emacs","ed-ed-emacs","emacs-emacs-emacs"),lty=1,pch=c(4,2,1),bg="white", pt.cex=1, cex=legend_scale)
        # title(maintxt, line=0.3)
        # dev.off()
        # 
        # #x11(width=6,height=6)
        # png(paste(savename,"_edted.png",sep=""), width=pwid,height=pht, units="in",res=300)
        # par(lwd=2, mar=c(3,3,1,1), mgp=c(2,0.5,0), cex.lab=1.2)
        # plot(1:6,dat.m["ED-ED-EMACS",],ylim=y_range,type="b",pch=2,xlab="Day",ylab=y_label)
        # abline(v=2.5,lty=3,lwd=1)
        # abline(v=4.5,lty=3,lwd=1)
        # lines(dat.m["EDT-ED-EMACS",],type="b",pch=2,lty=2)
        # legend(legend_x,legend_y,legend=c("edt-ed-emacs","ed-ed-emacs"),pch=2,lty=c(3,1),bg="white", pt.cex=1, cex=legend_scale)
        # title(maintxt, line=0.3)
        # dev.off()
        
        pCh <- ifelse(PLOT_SOURCE=="PRIMS",3, 1)
        png(paste(savename,"_ededt.png",sep=""), width=pwid,height=pht, units="in",res=300)
        par(lwd=2, mar=c(3,3,1,1), mgp=c(2,0.5,0), cex.lab=1.2)
        plot(1:6,dat.m["EDT-EDT-EMACS",],ylim=y_range,type="b",pch=pCh,xlab="Day",ylab=y_label,cex.axis=1,col="black")
        abline(v=2.5,lty=3,lwd=1)
        abline(v=4.5,lty=3,lwd=1)
        lines(dat.m["ED-EDT-EMACS",],type="b",pch=pCh,lty=2)
        
        legend(legend_x,legend_y,legend=c("edt-edt-emacs","ed-edt-emacs"),pch=c(pCh,pCh),lty=c(1,3),bg="white", pt.cex=1, cex=legend_scale,col=c("black","black"))
        title(maintxt, line=0.3)
        dev.off()
      }
    }
  }
  
}
