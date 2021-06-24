# R-file to plot data from the Karbach and Kray experiment and model

library(stringr)
library(Hmisc)
fig_w = 3
fig_h = 5
outdir = "/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/karbachkray/results/"

PLOT_SOURCE <- "PRIMS"    # "PROP2", "PROP3", "PRIMS", "HUMAN"
PLOT_SWEEP <- FALSE
PLOT_ERRBAR <- TRUE

DO_SWITCH <- TRUE         # Generate graph for the Task-switching task
DO_COUNT <- TRUE         # Generate graph for the Count-span task
DO_STROOP <- TRUE         # Generate graph for the Stroop task

indir <- "/home/bryan/Actransfer/supplemental/Actransfer distribution/KarbachKray/original/"
file_prefix <- ""
file_postfix <- ""
out_label <- "actransfer"
LR <- c("")
DR <- c("")

if (PLOT_SOURCE == "PROP3") {
  indir <- "/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/karbachkray/results/"
  file_prefix <- "KK_"
  out_label <- "prop3"
  t <- "1"
  s <- "21"
  
  LR <- if (PLOT_SWEEP) str_pad(2*(1:1), 2, pad="0") else c("02")        # A range of learning-rates from 0.02 to 0.2
  DR <- if (PLOT_SWEEP) str_pad(775+5*(0:0), 2, pad="0") else c("775")   # A range of discount-rates from 0.7 to 0.9
}


######################################

## Plot human data
if (PLOT_SOURCE == "HUMAN") {
  
  if (DO_SWITCH) {
    # Plot task-switching
    
    exp.switching <- (rbind(c(379,280),c(327,146))+rbind(c(217,174),c(200,104))+rbind(c(342,267),c(305,196)))/3
    #exp.switching <- rbind(c(385,280),c(325,140))
    exp.switching.se <- (rbind(c(426-379,324-280),c(368-327,185-146))*sqrt(14) +
                           rbind(c(269-217,227-174),c(227-200,134-104))*sqrt(14) +
                           rbind(c(408-341,301-267),c(333-305,225-196))*sqrt(14)) /3 / sqrt(42) 
    
    x11(width=fig_w,height=fig_h)
    #png(paste(outdir,"fig_kk_human_switch.png",sep=""), width=fig_w,height=fig_h, units="in",res=300)
    par(lwd=2)
    errbar(c(2,5),exp.switching[1,],exp.switching[1,]+exp.switching.se[1,],exp.switching[1,]-exp.switching.se[1,],ylim=c(0,600),type="b",xlab="",xaxt="n",xlim=c(1,6),ylab="Switching costs (ms)")
    errbar(c(2,5),exp.switching[2,],exp.switching[2,]+exp.switching.se[2,],exp.switching[2,]-exp.switching.se[2,],type="b",pch=2,add=T)
    
    #lines(4:5,dat.switching[1,]*1000,type="b")
    #lines(4:5,dat.switching[2,]*1000,type="b",pch=2)
    
    title(main="Task Switching")
    axis(1,at=c(2,5),labels=c("Pre-train","Post-train"))
    legend(2,600,legend=c("Control","Test"),pch=1:2,lty=1,bty="n")
    #dev.off()
  }
  
  if (DO_COUNT) {
    # Plot digitspan
    
    preSingle <- (47.3+66.1+56.3)/3
    preSwitch <- (45.8+71.6+55.1)/3
    postSingle <- (47.8+68.3+58)/3
    postSwitch <- (56+81.3+62.4)/3
    
    exp.data <- rbind(c(preSingle,postSingle),c(preSwitch,postSwitch))/100
    exp.data.se <- rbind(c(11.4+12.7+16.8,24+16.3+15.8),c(13.4+16.1+15.9,17.2+15.3+18.6))/300/sqrt(42)
    
    x11(width=fig_w,height=fig_h)
    #png(paste(outdir,"fig_kk_human_digitspan.png",sep=""), width=fig_w,height=fig_h, units="in",res=300)
    par(lwd=2)
    errbar(c(2,5),exp.data[1,],exp.data[1,]+exp.data.se[1,],exp.data[1,]-exp.data.se[1,],ylim=c(0.4,1),type="b",xlab="",xaxt="n",xlim=c(1,6),ylab="Proportion correct")
    errbar(c(2,5),exp.data[2,],exp.data[2,]+exp.data.se[2,],exp.data[2,]-exp.data.se[2,],type="b",pch=2,add=T)
    
    #lines(4:5,dat.m[1,],type="b")
    #lines(4:5,dat.m[2,],type="b",pch=2)
    
    title(main="Count Span")    
    axis(1,at=c(2,5),labels=c("Pre-train","Post-train"))
    legend(2,1,legend=c("Control","Test"),pch=1:2,lty=1,bty="n")
    #dev.off()
  }
  
  if (DO_STROOP) {
    # Plot Stroop
    
    preSingle <- (70+30+57)/3
    preSwitch <- (48+57+77)/3
    postSingle <- (72+48+72)/3
    postSwitch <- (24+27+56)/3
    
    exp.stroop <- rbind(c(preSingle,postSingle),c(preSwitch,postSwitch))
    exp.stroop.se <- rbind(c(42+31+57,49+41+55),c(61+41+81,53+34+46)) /3 /sqrt(42)
    
    x11(width=fig_w,height=fig_h)
    #png(paste(outdir,"fig_kk_human_stroop.png",sep=""), width=fig_w,height=fig_h, units="in",res=300)
    par(lwd=2)
    errbar(c(2,5),exp.stroop[1,],exp.stroop[1,]+exp.stroop.se[1,],exp.stroop[1,]-exp.stroop.se[1,],ylim=c(0,100),type="b",xlab="",xaxt="n",xlim=c(1,6),ylab="Interference (ms)")
    errbar(c(2,5),exp.stroop[2,],exp.stroop[2,]+exp.stroop.se[2,],exp.stroop[2,]-exp.stroop.se[2,],type="b",pch=2,add=T)
  
    #lines(4:5,(res[,1,1]-res[,1,2])*1000,type="b")
    #lines(4:5,(res[,2,1]-res[,2,2])*1000,type="b",pch=2)
    
    title(main="Stroop")  
    axis(1,at=c(2,5),labels=c("Pre-train","Post-train"))
    legend(2,100,legend=c("Control","Test"),pch=1:2,lty=1,bty="n")
    #dev.off()
  }
  
} else {

  ## Plot the model results
  
  for (lr in LR) {
    for (dr in DR) {
      
      file_postfix <- ifelse(PLOT_SOURCE == "PRIMS", "", paste("_l12_t",t,"_lr",lr,"_dr",dr,"_s",s,sep=""))
      
      if (DO_SWITCH) {
      # Plot task-switching
      
      dat <- read.table(paste(indir,file_prefix,"task-switching",file_postfix,".txt", sep=""))
      names(dat) <- c("condition","day","task","trialtype","trial","time")
      dat <- dat[dat$trial != 1,]
      dat.m <- with(dat[dat$day %in% c(1,6),],tapply(time,list(condition,day,trialtype),mean))
      dat.switch_n <- length(subset(dat, trialtype=="SWITCH", select=time))
      dat.repeat_n <- length(subset(dat, trialtype=="REPEAT", select=time))
      dat.sd <- with(dat[dat$day %in% c(1,6),],tapply(time,list(condition,day,trialtype),function(x) {sd(x)} ))
      dat.switching <- dat.m[,,"SWITCH"] - dat.m[,,"REPEAT"]
      dat.switching.se <- sqrt((dat.sd[,,"SWITCH"]^2)/dat.switch_n + (dat.sd[,,"REPEAT"]^2)/dat.repeat_n) # SEdiff = sqrt(sd_1^2/n_1 + sd_2^2/n_2)
      # DegFreedom = (n_1-1)+(n_2-1); t = abs(qt(0.05, DegFreedom)); 95% confidence interval = mean +- t*se
      
      #if (PLOT_SWEEP)
        x11(width=fig_w,height=fig_h)
      #else
      #  png(paste(outdir,"fig_kk_",out_label,"_switch",file_postfix,".png",sep=""), width=fig_w,height=fig_h, units="in",res=300)
      par(lwd=2)
      if (PLOT_ERRBAR) {
        errbar(c(2,5),dat.switching[1,]*1000,(dat.switching[1,]+dat.switching.se[1,])*1000,(dat.switching[1,]-dat.switching.se[1,])*1000,ylim=c(0,600),type="b",xlab="",xaxt="n",xlim=c(1,6),ylab="Switching costs (ms)")
        errbar(c(2,5),dat.switching[2,]*1000,(dat.switching[2,]+dat.switching.se[2,])*1000,(dat.switching[2,]-dat.switching.se[2,])*1000,type="b",pch=2,add=T)
      } else {
        plot(c(2,5),dat.switching[1,]*1000,type="b",xlab="",xaxt="n",ylim=c(0,600),xlim=c(1,6),ylab="Switching costs (ms)")
        lines(c(2,5),dat.switching[2,]*1000,type="b",pch=2)
      }
      if (!PLOT_SWEEP) {
        title(main="Task Switching")
      } else {
        title(main=paste("Task Switch: lr=",as.double(lr)*0.01," dr=",as.double(dr)*0.01,sep=""))
      }
      axis(1,at=c(2,5),labels=c("Pre-train","Post-train"))
      legend(2,600,legend=c("Control","Test"),pch=1:2,lty=1,bty="n")
      #if (!PLOT_SWEEP)
        #dev.off()
      }
      
      if (DO_COUNT) {
      # Plot digitspan
      
      dat <- read.table(paste(indir,file_prefix,"digitspan",file_postfix,".txt", sep=""))
      names(dat) <- c("day","condition","span","numresponded","numcorrect","accuracy")
      dat$mult <- dat$accuracy * dat$span
      dat.m <- with(dat,tapply(mult,list(condition,day),sum))
      dat.m <- dat.m/length(dat$day)
      
      #if (PLOT_SWEEP)
        x11(width=fig_w,height=fig_h)
      #else
      #  png(paste(outdir,"fig_kk_",out_label,"_digitspan",file_postfix,".png",sep=""), width=fig_w,height=fig_h, units="in",res=300)
      par(lwd=2)
      if (PLOT_ERRBAR) {
        errbar(c(2,5),exp.data[1,],exp.data[1,]+exp.data.se[1,],exp.data[1,]-exp.data.se[1,],ylim=c(0.4,1),type="b",xlab="",xaxt="n",xlim=c(1,6),ylab="Proportion correct")
        errbar(c(2,5),exp.data[2,],exp.data[2,]+exp.data.se[2,],exp.data[2,]-exp.data.se[2,],type="b",pch=2,add=T)
      } else {
        plot(c(2,5),dat.m[1,],ylim=c(0.4,1),type="b",xlab="",xaxt="n",xlim=c(1,6),ylab="Proportion correct")
        lines(c(2,5),dat.m[2,],type="b",pch=2)
      }
      title(main="Count Span")
      axis(1,at=c(2,5),labels=c("Pre-train","Post-train"))
      legend(2,1,legend=c("Control","Test"),pch=1:2,lty=1,bty="n")
      #if (!PLOT_SWEEP)
      #  dev.off()
      }
      
      if (DO_STROOP) {
        # Plot Stroop
        
        dat <- read.table(paste(indir,file_prefix,"stroop",file_postfix,".txt", sep=""))
        names(dat) <- c("task","condition","block","day","trial","type","correct","RT")
        dat.m <- with(dat,tapply(RT,list(day,condition,type),mean))  # Pre/Post,Control/Test,Neutral/Incongruent
        dat.inc_n <- length(subset(dat, type=="INCONGRUENT", select=RT))
        dat.neu_n <- length(subset(dat, type=="NEUTRAL", select=RT))
        dat.sd <- with(dat,tapply(RT,list(day,condition,type),function(x) {sd(x)} ))
        dat.interference <- dat.m[,,"INCONGRUENT"] - dat.m[,,"NEUTRAL"]
        dat.interference.se <- sqrt((dat.sd[,,"INCONGRUENT"]^2)/dat.inc_n + (dat.sd[,,"NEUTRAL"]^2)/dat.neu_n) # SEdiff = sqrt(sd_1^2/n_1 + sd_2^2/n_2)
        
        #if (PLOT_SWEEP)
          x11(width=fig_w,height=fig_h)
        #else
        #  png(paste(outdir,"fig_kk_",out_label,"_stroop",file_postfix,".png",sep=""), width=fig_w,height=fig_h, units="in",res=300)
        par(lwd=2)
        if (PLOT_ERRBAR) {
          errbar(c(2,5),dat.interference[,1]*1000,(dat.interference[,1]+dat.interference.se[,1])*1000,(dat.interference[,1]-dat.interference.se[,1])*1000,ylim=c(0,100),type="b",xlab="",xaxt="n",xlim=c(1,6),ylab="Interference (ms)")
          errbar(c(2,5),dat.interference[,2]*1000,(dat.interference[,2]+dat.interference.se[,2])*1000,(dat.interference[,2]-dat.interference.se[,2])*1000,type="b",pch=2,add=T)
        } else {
          plot(c(2,5),(dat.interference[,1])*1000,ylim=c(0,100),type="b",xlab="",xaxt="n",xlim=c(1,6),ylab="Interference (ms)")
          lines(c(2,5),(dat.interference[,2])*1000,type="b",pch=2)
        }
        
        if (!PLOT_SWEEP) {
          title(main="Stroop")
        } else {
          title(main=paste("lr=",as.double(lr)*0.01," dr=",as.double(dr)*0.01," se=",round(1000*dat.interference.se[2,2],digits=2),sep=""))
        }
        axis(1,at=c(2,5),labels=c("Pre-train","Post-train"))
        legend(2,100,legend=c("Control","Test"),pch=1:2,lty=1,bty="n")
        #if (!PLOT_SWEEP)
          #dev.off()
      }
      
    }
  }
}
