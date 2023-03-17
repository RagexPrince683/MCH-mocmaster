package mcheli.eval.eval.exp;

public abstract class Col1AfterExpression extends Col1Expression {

   protected Col1AfterExpression() {}

   protected Col1AfterExpression(Col1Expression from, ShareExpValue s) {
      super(from, s);
   }

   protected AbstractExpression replace() {
      super.exp = super.exp.replaceVar();
      return super.share.repl.replaceVar1(this);
   }

   protected AbstractExpression replaceVar() {
      return this.replace();
   }

   public String toString() {
      if(super.exp == null) {
         return this.getOperator();
      } else {
         StringBuffer sb = new StringBuffer();
         if(super.exp.getPriority() > super.prio) {
            sb.append(super.exp.toString());
            sb.append(this.getOperator());
         } else if(super.exp.getPriority() == super.prio) {
            sb.append(super.exp.toString());
            sb.append(' ');
            sb.append(this.getOperator());
         } else {
            sb.append(super.share.paren.getOperator());
            sb.append(super.exp.toString());
            sb.append(super.share.paren.getEndOperator());
            sb.append(this.getOperator());
         }

         return sb.toString();
      }
   }
}
