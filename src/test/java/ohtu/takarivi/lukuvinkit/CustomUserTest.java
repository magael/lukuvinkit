package ohtu.takarivi.lukuvinkit;

import ohtu.takarivi.lukuvinkit.domain.CustomUser;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CustomUserTest {

    CustomUser cu;

    @Test
    public void constructorNoException() {
        cu = new CustomUser("user", "password", "name");
    }

}
