package jp.co.sss.pr.login_function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.form.CustomerForm;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.service.LevelService;
import jp.co.sss.pr.service.PasswordService;

/** ログイン・ログアウト・新規登録の正常系／異常系 */
class LoginControllerFlowTest {

    private CustomerRepository customerRepository;
    private LevelService levelService;
    private PasswordService passwordService;
    private MockHttpSession session;
    private LoginController controller;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        levelService = mock(LevelService.class);
        passwordService = new PasswordService();
        session = new MockHttpSession();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);

        controller = new LoginController();
        controller.customerRepository = customerRepository;
        controller.answerRepository = mock(AnswerRepository.class);
        controller.levelService = levelService;
        controller.passwordService = passwordService;
        controller.session = session;
        controller.request = request;

        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
    }

    private CustomerForm form(String name, String pass) {
        CustomerForm form = new CustomerForm();
        form.setUserName(name);
        form.setUserPass(pass);
        return form;
    }

    private BindingResult validate(CustomerForm form) {
        BindingResult result = new BeanPropertyBindingResult(form, "customerForm");
        validator.validate(form, result);
        return result;
    }

    private Customer customer(int id, String name, String rawPass, boolean deleted) {
        Customer c = new Customer();
        c.setUserId(id);
        c.setUserName(name);
        c.setUserPass(passwordService.encode(rawPass));
        c.setPermission(0);
        c.setDeleteFlag(deleted);
        return c;
    }

    // ---- 画面表示 ----

    @Test
    void topPageInvalidatesTheSession() {
        assertEquals("login/index", controller.index(new CustomerForm()));
        assertTrue(session.isInvalid());
    }

    @Test
    void loginAndRegistPagesAreShown() {
        assertEquals("login/login", controller.loginInput(new CustomerForm()));
        assertEquals("login/user_regist", controller.inputRegist(new CustomerForm()));
    }

    @Test
    void loginErrorPageClearsTheSession() {
        assertEquals("login_error", controller.loginError(new CustomerForm()));
        assertTrue(session.isInvalid());
    }

    // ---- ログイン ----

    @Test
    void loginSucceedsAndStoresASessionUserWithoutPassword() {
        Customer c = customer(1, "taro", "pass1234", false);
        when(customerRepository.findAllByUserName("taro")).thenReturn(List.of(c));
        when(levelService.processLogin(c)).thenReturn(List.of());
        CustomerForm form = form("taro", "pass1234");

        String view = controller.login(form, validate(form), session, new ExtendedModelMap());

        assertEquals("redirect:/mypage", view);
        CustomerBean bean = (CustomerBean) session.getAttribute("user");
        assertNotNull(bean);
        assertEquals("taro", bean.getUserName());
        verify(levelService).processLogin(c);
    }

    @Test
    void loginWithBlankFieldsReturnsToLoginWithoutLookup() {
        CustomerForm form = form("", "");

        String view = controller.login(form, validate(form), session, new ExtendedModelMap());

        assertEquals("login/login", view);
        verify(customerRepository, never()).findAllByUserName(any());
    }

    @Test
    void loginWithMalformedPasswordIsRejectedByValidation() {
        CustomerForm form = form("taro", "ab");
        BindingResult result = validate(form);

        assertTrue(result.hasFieldErrors("userPass"));
        assertEquals("login/login", controller.login(form, result, session, new ExtendedModelMap()));
    }

    @Test
    void loginWithUnknownUserShowsErrorAndDoesNotLogIn() {
        when(customerRepository.findAllByUserName("nobody")).thenReturn(List.of());
        CustomerForm form = form("nobody", "pass1234");
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.login(form, validate(form), session, model);

        assertEquals("login/login", view);
        assertNotNull(model.get("errMessage"));
        assertNull(session.getAttribute("user"));
    }

    @Test
    void loginWithWrongPasswordIsRejected() {
        when(customerRepository.findAllByUserName("taro"))
                .thenReturn(List.of(customer(1, "taro", "pass1234", false)));
        CustomerForm form = form("taro", "wrong999");
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("login/login", controller.login(form, validate(form), session, model));
        assertNull(session.getAttribute("user"));
        verify(levelService, never()).processLogin(any());
    }

    @Test
    void loginWithDeletedUserIsRejected() {
        when(customerRepository.findAllByUserName("taro"))
                .thenReturn(List.of(customer(1, "taro", "pass1234", true)));
        CustomerForm form = form("taro", "pass1234");

        assertEquals("login/login",
                controller.login(form, validate(form), session, new ExtendedModelMap()));
        assertNull(session.getAttribute("user"));
    }

    @Test
    void loginRestoresSavedBackgroundColor() {
        Customer c = customer(1, "taro", "pass1234", false);
        c.setBackgroundClass("bg-color");
        c.setBackgroundColor("#AABBCC");
        when(customerRepository.findAllByUserName("taro")).thenReturn(List.of(c));
        when(levelService.processLogin(c)).thenReturn(List.of());
        CustomerForm form = form("taro", "pass1234");

        controller.login(form, validate(form), session, new ExtendedModelMap());

        assertEquals("bg-color", session.getAttribute("backgroundClass"));
        assertEquals("#AABBCC", session.getAttribute("backgroundColor"));
    }

    @Test
    void loginIgnoresAnInvalidSavedBackgroundColor() {
        Customer c = customer(1, "taro", "pass1234", false);
        c.setBackgroundClass("bg-color");
        c.setBackgroundColor("red; x");
        when(customerRepository.findAllByUserName("taro")).thenReturn(List.of(c));
        when(levelService.processLogin(c)).thenReturn(List.of());
        CustomerForm form = form("taro", "pass1234");

        controller.login(form, validate(form), session, new ExtendedModelMap());

        assertNull(session.getAttribute("backgroundClass"));
        assertNull(session.getAttribute("backgroundColor"));
    }

    // ---- ログアウト ----

    @Test
    void logoutClearsLoginFlagAndInvalidatesTheSession() {
        Customer c = customer(1, "taro", "pass1234", false);
        c.setLoginFlag(true);
        CustomerBean bean = new CustomerBean();
        bean.setUserId(1);
        session.setAttribute("user", bean);
        when(customerRepository.findById(1)).thenReturn(Optional.of(c));

        assertEquals("redirect:/", controller.logout());

        assertFalse(c.getLoginFlag());
        verify(customerRepository).save(c);
        assertTrue(session.isInvalid());
    }

    @Test
    void logoutWithoutLoginJustRedirects() {
        assertEquals("redirect:/", controller.logout());
        verify(customerRepository, never()).save(any());
    }

    // ---- 新規登録 ----

    @Test
    void registerCreatesAGeneralUserWithHashedPasswordAndLogsIn() {
        when(customerRepository.existsByUserName("hanako")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> {
            Customer saved = i.getArgument(0);
            saved.setUserId(7);
            return saved;
        });
        CustomerForm form = form("hanako", "pass1234");

        String view = controller.checkRegist(form, validate(form), new ExtendedModelMap());

        assertEquals("redirect:/mypage", view);
        org.mockito.ArgumentCaptor<Customer> captor = org.mockito.ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        Customer saved = captor.getValue();
        assertEquals(0, saved.getPermission());
        assertEquals(1, saved.getUserLevel());
        assertFalse(saved.getDeleteFlag());
        assertFalse("pass1234".equals(saved.getUserPass()));
        assertTrue(passwordService.matches("pass1234", saved.getUserPass()));
        assertEquals(7, ((CustomerBean) session.getAttribute("user")).getUserId());
    }

    @Test
    void registerRejectsAnExistingUserName() {
        when(customerRepository.existsByUserName("taro")).thenReturn(true);
        CustomerForm form = form("taro", "pass1234");
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.checkRegist(form, validate(form), model);

        assertEquals("login/user_regist", view);
        assertEquals("既に使用されているユーザー名です。", model.get("errMessage"));
        verify(customerRepository, never()).save(any());
        assertNull(session.getAttribute("user"));
    }

    @Test
    void registerRejectsBlankName() {
        CustomerForm form = form("", "pass1234");

        assertEquals("login/user_regist",
                controller.checkRegist(form, validate(form), new ExtendedModelMap()));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void registerRejectsTooLongName() {
        CustomerForm form = form("123456789", "pass1234");
        BindingResult result = validate(form);

        assertTrue(result.hasFieldErrors("userName"));
        assertEquals("login/user_regist", controller.checkRegist(form, result, new ExtendedModelMap()));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void registerRejectsShortLongAndNonAlphanumericPasswords() {
        for (String bad : new String[] { "abc", "12345678901234567", "pass 123", "パスワード1234" }) {
            CustomerForm form = form("hanako", bad);
            BindingResult result = validate(form);
            assertTrue(result.hasFieldErrors("userPass"), "パスワード「" + bad + "」は不可");
            assertEquals("login/user_regist",
                    controller.checkRegist(form, result, new ExtendedModelMap()));
        }
        verify(customerRepository, never()).save(any());
    }

    @Test
    void registerAcceptsBoundaryLengths() {
        assertFalse(validate(form("a", "abcd")).hasErrors());
        assertFalse(validate(form("12345678", "abcdEFGH12345678")).hasErrors());
    }
}
