package test.eh.prepay;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class Ptest {

    public static void main(String[] args) {
        A a = new A();
        String test = Optional.ofNullable(a).map(A::getB).map(B::getTest).orElseGet(() -> "2");
        System.out.println(test);

        A a1 = null;
        String test1 = Optional.ofNullable(a1).map(A::getB).map(B::getTest).orElseGet(() -> "2");
        System.out.println(test1);

        A a2 = new A();
        B b = new B();
        a2.setB(b);
        b.setTest("sxy");
        String test2 = Optional.ofNullable(a2).map(A::getB).map(B::getTest).orElseGet(() -> test("2"));
        System.out.println(test2);

        Optional.ofNullable(a2).ifPresent(a4 -> b.setTest(a4.getB().getTest()));
        System.out.println(b);

        List<A> list = new LinkedList<>();
        Optional.ofNullable(list).ifPresent(a7 -> b.setTest("111"));
    }

    private static String test(String test) {
        System.out.println("test" + test);
        return test;
    }
}

@Getter
@Setter
class A {
    private B b;
}

@Getter
@Setter
class B {
    private String test;
}