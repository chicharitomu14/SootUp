/* Soot - a J*va Optimization Framework
 * Copyright (C) 1999 Patrick Lam
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/*
 * Modified by the Sable Research Group and others 1997-1999.
 * See the 'credits' file distributed with Soot for the complete list of
 * contributors.  (Soot is distributed at http://www.sable.mcgill.ca/soot)
 */

package de.upb.swt.soot.core.jimple.common.expr;

import de.upb.swt.soot.core.jimple.Jimple;
import de.upb.swt.soot.core.jimple.basic.Value;
import de.upb.swt.soot.core.jimple.basic.ValueBox;
import de.upb.swt.soot.core.types.PrimitiveType;
import de.upb.swt.soot.core.types.Type;
import de.upb.swt.soot.core.types.UnknownType;

@SuppressWarnings("serial")
public abstract class AbstractFloatBinopExpr extends AbstractBinopExpr {

  AbstractFloatBinopExpr(Value op1, Value op2) {
    this(Jimple.newArgBox(op1), Jimple.newArgBox(op2));
  }

  private AbstractFloatBinopExpr(ValueBox op1Box, ValueBox op2Box) {
    super(op1Box, op2Box);
  }

  @Override
  public Type getType() {
    Value op1 = getOp1();
    Value op2 = getOp2();
    Type op1t = op1.getType();
    Type op2t = op2.getType();
    if ((op1t.equals(PrimitiveType.getInt())
            || op1t.equals(PrimitiveType.getByte())
            || op1t.equals(PrimitiveType.getShort())
            || op1t.equals(PrimitiveType.getChar())
            || op1t.equals(PrimitiveType.getBoolean()))
        && (op2t.equals(PrimitiveType.getInt())
            || op2t.equals(PrimitiveType.getByte())
            || op2t.equals(PrimitiveType.getShort())
            || op2t.equals(PrimitiveType.getChar())
            || op2t.equals(PrimitiveType.getBoolean()))) {
      return PrimitiveType.getInt();
    } else if (op1t.equals(PrimitiveType.getLong()) || op2t.equals(PrimitiveType.getLong())) {
      return PrimitiveType.getLong();
    } else if (op1t.equals(PrimitiveType.getDouble()) || op2t.equals(PrimitiveType.getDouble())) {
      return PrimitiveType.getDouble();
    } else if (op1t.equals(PrimitiveType.getFloat()) || op2t.equals(PrimitiveType.getFloat())) {
      return PrimitiveType.getFloat();
    } else {
      return UnknownType.getInstance();
    }
  }
}