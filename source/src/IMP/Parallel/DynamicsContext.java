package IMP.Parallel;

import IMP.Translate.ContextWithVarLink;

/**
 * HML IMP.Parallel
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-11-6.
 */
public class DynamicsContext {


    /*
    The visitor
     */
    private IncrementalVisitor incrementalVisitor;
    private ContextWithVarLink continuous;
    private Thread thread;


    public DynamicsContext(IncrementalVisitor incrementalVisitor) {
        this.incrementalVisitor = incrementalVisitor;
    }

    public DynamicsContext(IncrementalVisitor incrementalVisitor, ContextWithVarLink continuous, Thread thread) {
        this.incrementalVisitor = incrementalVisitor;
        this.continuous = continuous;
        this.thread = thread;
    }

    public IncrementalVisitor getIncrementalVisitor() {
        return incrementalVisitor;
    }

    public void setIncrementalVisitor(IncrementalVisitor incrementalVisitor) {
        this.incrementalVisitor = incrementalVisitor;
    }

    public ContextWithVarLink getContinuous() {
        return continuous;
    }

    public void setContinuous(ContextWithVarLink continuous) {
        this.continuous = continuous;
    }


    public Thread getThread() {
        return thread;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }
}
