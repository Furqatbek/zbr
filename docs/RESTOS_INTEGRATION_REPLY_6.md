# Reply to Restos — round six

**Draft for review.** Nothing blocks sending. The 8% is still ours to answer and
is on the call agenda rather than in this reply.

---

All three points taken, and two of them are now mechanisms rather than
intentions. The `CANCELLED` collision is the one that needed a decision, and we
think yours is right — with one addition.

## `expectedTotal`

Already `subtotal + deliveryFee` on our side; your round-four note landed before
this reply went out. No apology needed for the mapping table — it was right for
the payload we had sent you, and our total grew a tax line after it.

Your hint table is better than the formula. Two ways to get it wrong, each
answering itself, and silence on a genuinely stale price so nobody goes looking
in the wrong system. We have kept the reasoning in our code rather than the
number, so the next person to touch it inherits the why rather than a constant.

**We are keeping it.** Your point stands: the thing that let the counter-price
import run for months was that no number on either side had to agree with
anything. `expectedTotal` is where the two catalogues agree out loud, and
dropping it for being awkward would be removing the one check that would have
caught it.

## `PREPAID` — now a switch, not a promise

You were right to be precise, and the consequence you describe is the one that
made us change how this is enforced rather than just noted.

**A venue cannot be switched on for order push while `paymentMode` is a
constant.** It is a configuration flag we set the day our apps send the field
for real, and until then the grant endpoint refuses `pushOrders: true` and says
why. Menu writes and status reports are unaffected, exactly as you suggested.

You said you cannot detect the change from your side. Neither could we, reliably
— a promise someone has to remember is worse than a switch, and this had a
person's memory standing between a constant and a venue giving away food.

**We will tell you the day it flips**, as a message rather than as traffic. And
yes: if any venue turns out to take cash before the apps are fixed, we will say
so and hold their push rather than decide on their behalf.

## `CANCELLED` meets your cutoff — agreed, with one addition

Your reading is right and we would not argue with any of the four steps. The
`422` is the signal, a `200` would say the opposite silently, and the restaurant
would absorb the food without anyone having decided that they should.

**So we have built step 3 rather than promising it.** A cancellation you refuse
is now recorded against the order as *the venue is owed for this ticket*, with
your reason attached, in its own column and its own log line. It is not a retry
and not an integration fault, and a redelivered message cannot count it twice.

The addition is the reason we built it now rather than after the call: **the
commercial answer does not exist yet, and a log line cannot be settled against
later.** If we had waited, the answer would only have been applicable to orders
placed after it was decided. As it stands, whatever the call concludes can be
applied to every one of these from the first.

You put it as not wanting to change the meaning of a status code afterwards. The
same argument applies one layer down: we would rather not have to reconstruct
which tickets these were.

We would rather keep the refusal than accept-and-mark-late. Accepting would make
our two systems agree on a screen while disagreeing about money, which is the
version of this that gets discovered late.

## The counter prices

Thank you. We would rather have told you and been wrong about it mattering.

Your observation is the one we took away too: nothing on either side could
answer "which prices are these", and a number is a number. That is now three
findings from this integration that were invisible until someone made two
systems state the same fact twice — the prices, the variants, and
`expectedTotal` itself.

**We will run the sync as the end-to-end test, as you suggest**, diff our
catalogue against the menu you serve, and treat any surviving difference as a
bug in one of us before a customer sees either catalogue. That is a better first
test than an order push, and we would not have sequenced it that way.

## Staging

Nothing waiting on us. When your environment is up we will want the venue id,
the base URL and the key in the one message, and we will create the partner
record, a `staging` key and the grant that day.

Order push stays off on that grant — now for two reasons rather than one, since
the `paymentMode` switch would refuse it anyway. We will switch it on with a
person watching both ends, after the catalogue diff is clean.

## The call

The three stand, and your framing of the first is right: we can surface a failed
refund to the venue because we are the only ones who learn it failed, and who
carries it is not a schema question.

The 8% belongs there for the same reason. We do not have the answer yet and we
are not going to manufacture one for a document — what it is, is a matter of
record somewhere in this company, and we would rather find out than guess. Your
markup editor showing the venue the whole chain, labelled as our figure, is the
right handling in the meantime and better than anything we proposed.

Agreed that each of the three has a restaurant on the other end, and agreed we
would rather answer them than meet them.
