package IMP.Parallel;


import AntlrGen.HMLBaseVisitor;
import AntlrGen.HMLParser;
import IMP.Translate.DynamicalVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * HML IMP.Parallel
 * Created by Huixing Fang (fang.huixing@gmail.com) on 14-11-6.
 */
public class IncrementalVisitor extends HMLBaseVisitor<Void> implements Runnable{


    private static Logger logger = LogManager.getLogger(IncrementalVisitor.class);
    private static int sleepTime = 100000;
    private ParserRuleContext currentCtx;
    private ParserRuleContext initCtx;
    private Thread thread;
    private boolean condition = true;

    public IncrementalVisitor(ParserRuleContext initCtx) {
        this.initCtx = initCtx;
    }

    public Void visitAtomPro(HMLParser.AtomProContext ctx) {
        if (ctx.atom() instanceof HMLParser.SkipContext)
            return null;

        setCurrentCtx(ctx.atom());
        finishOneCtx();
        return null;
    }

    public Void visitTemplate(HMLParser.TemplateContext ctx) {
        DynamicalVisitor.setCurrentScope(ctx);
        visit(ctx.parStatement());
        return null;
    }






    public Void visitSeqCom(HMLParser.SeqComContext ctx) {
        for (HMLParser.BlockStatementContext bs : ctx.blockStatement())
            visit(bs);
        return null;
    }


    public Void visitConChoice(HMLParser.ConChoiceContext ctx) {
        setCurrentCtx(ctx.expr());
        finishOneCtx();
        return null;
    }

    public Void visitOde(HMLParser.OdeContext ctx) {
        setCurrentCtx(ctx);
        finishOneCtx();
        return null;
    }


    public Void visitWhenPro(HMLParser.WhenProContext ctx) {
        setCurrentCtx(ctx);

        finishOneCtx();
        return null;
    }

    public Void visitLoopPro(HMLParser.LoopProContext ctx) {
        while (condition) {
            setCurrentCtx(ctx);
            finishOneCtx();
        }

        return null;
    }

    public Void visitCallTem(HMLParser.CallTemContext ctx) {
        setCurrentCtx(ctx);
        finishOneCtx();
        DynamicalVisitor.PopVariableStack();
        return null;
    }

    public Void visitParPro(HMLParser.ParProContext ctx) {
        visit(ctx.parStatement().blockStatement());
        return null;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p/>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        visit(initCtx);
    }


    private void finishOneCtx() {
        while (true) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                logger.info(e.getMessage());
                break;
            }
        }
    }

    public ParserRuleContext getCurrentCtx() {
        return currentCtx;
    }

    public void setCurrentCtx(ParserRuleContext currentCtx) {
        this.currentCtx = currentCtx;
    }

    public Thread getThread() {
        return thread;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    public boolean isCondition() {
        return condition;
    }

    public void setCondition(boolean condition) {
        this.condition = condition;
    }
}
