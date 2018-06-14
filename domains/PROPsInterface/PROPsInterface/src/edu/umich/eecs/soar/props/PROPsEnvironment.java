package edu.umich.eecs.soar.props;

public class PROPsEnvironment {

	Kernel kernel = Kernel.CreateKernelInNewThread();
	kernel.SetAutoCommit(false);
}
