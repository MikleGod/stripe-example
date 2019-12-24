package by.polyansky.stripe.example.controllers;

import by.polyansky.stripe.example.commons.Response;
import by.polyansky.stripe.example.services.StripeService;
import com.stripe.model.Coupon;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PaymentController {
    
    /*
    try {
        // Use Stripe's library to make requests...
    } catch (CardException e) {
        // Since it's a decline, CardException will be caught
        System.out.println("Status is: " + e.getCode());
        System.out.println("Message is: " + e.getMessage());
    } catch (RateLimitException e) {
        // Too many requests made to the API too quickly
    } catch (InvalidRequestException e) {
        // Invalid parameters were supplied to Stripe's API
    } catch (AuthenticationException e) {
        // Authentication with Stripe's API failed
        // (maybe you changed API keys recently)
    } catch (APIConnectionException e) {
        // Network communication with Stripe failed
    } catch (StripeException e) {
        // Display a very generic error to the user, and maybe send
        // yourself an email
    } catch (Exception e) {
        // Something else happened, completely unrelated to Stripe
    }
    */

    @Value("${stripe.keys.public}")
    private String API_PUBLIC_KEY;

    private StripeService stripeService;

    public PaymentController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @GetMapping("/")
    public String homepage() {
        return "homepage";
    }

    @GetMapping("/subscription")
    public String subscriptionPage(Model model) {
        model.addAttribute("stripePublicKey", API_PUBLIC_KEY);
        return "subscription";
    }

    @GetMapping("/charge")
    public String chargePage(Model model) {
        model.addAttribute("stripePublicKey", API_PUBLIC_KEY);
        return "charge";
    }

    @PostMapping("/create-subscription")
    public @ResponseBody
    Response createSubscription(String email, String token, String plan, String coupon) {
        if (token == null || plan.isEmpty()) {
            return new Response(false, "Stripe payment token is missing. Please, try again later.");
        }

        String customerId = stripeService.createCustomer(email, token);

        if (customerId == null) {
            return new Response(false, "An error occurred while trying to create a customer.");
        }

        String subscriptionId = stripeService.createSubscription(customerId, plan, coupon);
        if (subscriptionId == null) {
            return new Response(false, "An error occurred while trying to create a subscription.");
        }

        return new Response(true, "Success! Your subscription id is " + subscriptionId);
    }

    @PostMapping("/cancel-subscription")
    public @ResponseBody
    Response cancelSubscription(String subscriptionId) {
        boolean status = stripeService.cancelSubscription(subscriptionId);
        if (!status) {
            return new Response(false, "Failed to cancel the subscription. Please, try later.");
        }
        return new Response(true, "Subscription cancelled successfully.");
    }

    @PostMapping("/coupon-validator")
    public @ResponseBody
    Response couponValidator(String code) {
        Coupon coupon = stripeService.retrieveCoupon(code);
        if (coupon != null && coupon.getValid()) {
            String details = (coupon.getPercentOff() == null ? "$" + (coupon.getAmountOff() / 100) : coupon.getPercentOff() + "%") +
                    " OFF " + coupon.getDuration();
            return new Response(true, details);
        } else {
            return new Response(false, "This coupon code is not available. This may be because it has expired or has " +
                    "already been applied to your account.");
        }
    }

    @PostMapping("/create-charge")
    public @ResponseBody
    Response createCharge(String email, String token) {
        if (token == null) {
            return new Response(false, "Stripe payment token is missing. Please, try again later.");
        }

        String chargeId = stripeService.createCharge(email, token, 999); //$9.99 USD
        if (chargeId == null) {
            return new Response(false, "An error occurred while trying to create a charge.");
        }

        return new Response(true, chargeId);
    }

    @PostMapping("/charge-status")
    public @ResponseBody
    Response chargeStatus(String chargeId) {
        return new Response(true, stripeService.chargeStatus(chargeId));// success, pending, failed, null
    }

    @PostMapping("/subscription-status")
    public @ResponseBody
    Response subscriptionStatus(String subscriptionStatus) {
        return new Response(true, stripeService.subscriptionStatus(subscriptionStatus));
        //incomplete, incomplete_expired, trialing, active, past_due, canceled, or unpaid.
        /*
        Possible values are incomplete, incomplete_expired, trialing, active, past_due, canceled, or unpaid.
        For collection_method=charge_automatically a subscription moves into incomplete if the initial payment attempt fails.
        A subscription in this state can only have metadata and default_source updated.
         Once the first invoice is paid, the subscription moves into an active state.
          If the first invoice is not paid within 23 hours, the subscription transitions to incomplete_expired.
           This is a terminal state, the open invoice will be voided and no further invoices will be generated.
           A subscription that is currently in a trial period is trialing and moves to active when the trial period is over.
           If subscription collection_method=charge_automatically it becomes past_due when payment to renew it fails and canceled or unpaid
           (depending on your subscriptions settings) when Stripe has exhausted all payment retry attempts.
           If subscription collection_method=send_invoice it becomes past_due when its invoice is not paid by the due date,
            and canceled or unpaid if it is still not paid by an additional deadline after that.
             Note that when a subscription has a status of unpaid, no subsequent invoices will be attempted
             (invoices will be created, but then immediately automatically closed).
              After receiving updated payment information from a customer,
               you may choose to reopen and pay their closed invoices.
         */
    }
}
