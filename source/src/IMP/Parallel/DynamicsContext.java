package IMP.Parallel;

import IMP.Translate.ContextWithVarLink;

/**
 * HML IMP.Parallel
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-11-6.
 */
public class DynamicsContext {

    private IncrementalVisitor incrementalVisitor;
    private ContextWithVarLink continous;
    private Thread thread;


    public DynamicsContext(IncrementalVisitor incrementalVisitor) {
        this.incrementalVisitor = incrementalVisitor;
    }

    public DynamicsContext(IncrementalVisitor incrementalVisitor, ContextWithVarLink continous, Thread thread) {
        this.incrementalVisitor = incrementalVisitor;
        this.continous = continous;
        this.thread = thread;
    }

    public IncrementalVisitor getIncrementalVisitor() {
        return incrementalVisitor;
    }

    public void setIncrementalVisitor(IncrementalVisitor incrementalVisitor) {
        this.incrementalVisitor = incrementalVisitor;
    }

    public ContextWithVarLink getContinous() {
        return continous;
    }

    public void setContinous(ContextWithVarLink continous) {
        this.continous = continous;
    }


    public Thread getThread() {
        return thread;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }
}
