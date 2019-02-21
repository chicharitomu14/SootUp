/*-
 * #%L
 * Soot
 * %%
 * Copyright (C) 15.11.2018 Markus Schmidt
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package de.upb.soot.jimple.common.stmt;

import de.upb.soot.jimple.basic.Local;
import de.upb.soot.jimple.basic.PositionInfo;
import de.upb.soot.jimple.common.ref.JParameterRef;
import de.upb.soot.jimple.common.type.IntType;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import categories.Java8Test;

/**
 *
 * @author Markus Schmidt & Linghui Luo
 *
 */
@Category(Java8Test.class)
public class JNopStmtTest {

  @Test
  public void test() {
    PositionInfo nopos = PositionInfo.createNoPositionInfo();
    IStmt nop = new JNopStmt(nopos);

    Assert.assertTrue(nop.equivTo(nop));
    Assert.assertTrue(nop.equivTo(new JNopStmt(nopos)));

    Assert.assertFalse(
        nop.equivTo(new JIdentityStmt(new Local("$i0", IntType.INSTANCE), new JParameterRef(IntType.INSTANCE, 123), nopos)));

    Assert.assertEquals("nop", nop.toString());

  }

}
