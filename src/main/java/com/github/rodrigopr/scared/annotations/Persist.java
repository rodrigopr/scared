package com.github.rodrigopr.scared.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Goshme Soluções para a Internet LTDA<br />
 * Projeto JusBrasil<br />
 * <a href="http://www.jusbrasil.com.br/">http://www.jusbrasil.com.br/</a>
 * </p>
 *
 * @author <a href="mailto:rodriguinho@jusbrasil.com.br>Rodrigo Pereira Ribeiro</a>
 * @since 28/12/2012
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Persist {
  String name();

  Index[] customIndexes() default {};
}
