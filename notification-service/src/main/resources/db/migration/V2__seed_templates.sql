INSERT INTO notification_templates (id, code, channel, subject, body) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'booking.created', 'EMAIL', 'Booking pending payment', 'Your booking {{aggregateId}} is pending payment.'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'booking.confirmed', 'EMAIL', 'Booking confirmed', 'Your booking {{aggregateId}} is confirmed.'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', 'booking.cancelled', 'EMAIL', 'Booking cancelled', 'Your booking {{aggregateId}} was cancelled.'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', 'booking.expired', 'EMAIL', 'Booking expired', 'Your booking {{aggregateId}} expired before payment.'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5', 'payment.succeeded', 'EMAIL', 'Payment received', 'Payment {{aggregateId}} succeeded.'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6', 'payment.failed', 'EMAIL', 'Payment failed', 'Payment {{aggregateId}} failed.'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7', 'booking.confirmed.inapp', 'IN_APP', 'Booking confirmed', 'Booking {{aggregateId}} is confirmed.');
