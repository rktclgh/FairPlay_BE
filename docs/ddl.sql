CREATE TABLE user_role_code (
                                user_role_code_id INT PRIMARY KEY AUTO_INCREMENT,
                                code VARCHAR(20) NOT NULL UNIQUE,
                                name VARCHAR(50) NOT NULL
);

INSERT INTO user_role_code (code, name) VALUES
                                            ('ADMIN', '전체 관리자'),
                                            ('EVENT_MANAGER', '행사 담당자'),
                                            ('BOOTH_MANAGER', '부스 담당자'),
                                            ('COMMON', '일반 사용자');

CREATE TABLE apply_status_code (
                                   apply_status_code_id INT PRIMARY KEY AUTO_INCREMENT,
                                   code VARCHAR(20) NOT NULL UNIQUE,
                                   name VARCHAR(50) NOT NULL
);

INSERT INTO apply_status_code (code, name) VALUES
                                               ('PENDING', '대기'),
                                               ('APPROVED', '승인'),
                                               ('REJECTED', '거부');

CREATE TABLE event_status_code (
                                   event_status_code_id INT PRIMARY KEY AUTO_INCREMENT,
                                   code VARCHAR(20) NOT NULL UNIQUE,
                                   name VARCHAR(50) NOT NULL
);

INSERT INTO event_status_code (code, name) VALUES
                                               ('UPCOMING', '예정된 행사'),
                                               ('ONGOING', '진행중인 행사'),
                                               ('ENDED', '종료된 행사');

CREATE TABLE region_code (
                             region_code_id INT PRIMARY KEY AUTO_INCREMENT,
                             code VARCHAR(20) NOT NULL UNIQUE,
                             name VARCHAR(50) NOT NULL
);

INSERT INTO region_code (code, name) VALUES
                                         ('SEOUL', '서울'),
                                         ('GYEONGGI', '경기'),
                                         ('INCHEON', '인천'),
                                         ('GANGWON', '강원'),
                                         ('BUSAN', '부산'),
                                         ('GYEONGNAM', '경남'),
                                         ('DAEGU', '대구'),
                                         ('GYEONGBUK', '경북'),
                                         ('DAEJEON', '대전'),
                                         ('CHUNGNAM', '충남'),
                                         ('CHUNGBUK', '충북'),
                                         ('GWANGJU', '광주'),
                                         ('JEONBUK', '전북'),
                                         ('JEONNAM', '전남'),
                                         ('JEJU', '제주'),
                                         ('ULSAN', '울산'),
                                         ('OVERSEAS', '해외');

CREATE TABLE ticket_status_code (
                                    ticket_status_code_id INT PRIMARY KEY AUTO_INCREMENT,
                                    code VARCHAR(20) NOT NULL UNIQUE,
                                    name VARCHAR(50) NOT NULL
);

INSERT INTO ticket_status_code (code, name) VALUES
                                                ('BEFORE_SALE', '판매전'),
                                                ('SELLING', '판매중'),
                                                ('SOLD_OUT', '품절'),
                                                ('CLOSED', '판매 종료');

CREATE TABLE reservation_status_code (
                                         reservation_status_code_id INT PRIMARY KEY AUTO_INCREMENT,
                                         code VARCHAR(20) NOT NULL UNIQUE,
                                         name VARCHAR(50) NOT NULL
);

INSERT INTO reservation_status_code (code, name) VALUES
                                                     ('PENDING', '대기'),
                                                     ('CONFIRMED', '확정'),
                                                     ('CANCELLED', '취소'),
                                                     ('REFUNDED', '환불');

CREATE TABLE attendee_type_code (
                                    attendee_type_code_id INT PRIMARY KEY AUTO_INCREMENT,
                                    code VARCHAR(20) NOT NULL UNIQUE,
                                    name VARCHAR(50) NOT NULL
);

INSERT INTO attendee_type_code (code, name) VALUES
                                                ('PRIMARY', '대표자'),
                                                ('GUEST', '동반자');

CREATE TABLE payment_type_code (
                                   payment_type_code_id INT PRIMARY KEY AUTO_INCREMENT,
                                   code VARCHAR(20) NOT NULL UNIQUE,
                                   name VARCHAR(50) NOT NULL
);

INSERT INTO payment_type_code (code, name) VALUES
                                               ('CARD', '카드'),
                                               ('ACCOUNT', '계좌이체'),
                                               ('POINT', '포인트');

CREATE TABLE payment_status_code (
                                     payment_status_code_id INT PRIMARY KEY AUTO_INCREMENT,
                                     code VARCHAR(20) NOT NULL UNIQUE,
                                     name VARCHAR(50) NOT NULL
);

INSERT INTO payment_status_code (code, name) VALUES
                                                 ('PENDING', '결제 대기'),
                                                 ('COMPLETED', '결제 완료'),
                                                 ('FAILED', '결제 실패'),
                                                 ('CANCELLED', '결제 취소');


CREATE TABLE refund_status_code (
                                    refund_status_code_id INT PRIMARY KEY AUTO_INCREMENT,
                                    code VARCHAR(20) NOT NULL UNIQUE,
                                    name VARCHAR(50) NOT NULL
);

INSERT INTO refund_status_code (code, name) VALUES
                                                ('REQUESTED', '요청됨'),
                                                ('APPROVED', '승인됨'),
                                                ('COMPLETED', '환불완료'),
                                                ('REJECTED', '반려됨');

CREATE TABLE qr_action_code (
                                qr_action_code_id INT PRIMARY KEY AUTO_INCREMENT,
                                code VARCHAR(20) NOT NULL UNIQUE,
                                name VARCHAR(100) NOT NULL
);

INSERT INTO qr_action_code (code, name) VALUES
                                            ('ISSUED', '발급'),
                                            ('SCANNED', '스캔됨'),
                                            ('CHECKED_IN', 'QR 입장완료'),
                                            ('MANUAL_CHECKED_IN','수동 입장완료'),                                          
                                            ('INVALID', '유효하지 않음');

CREATE TABLE banner_status_code (
                                    banner_status_code_id INT PRIMARY KEY AUTO_INCREMENT,
                                    code VARCHAR(20) NOT NULL UNIQUE,
                                    name VARCHAR(50) NOT NULL
);

INSERT INTO banner_status_code (code, name) VALUES
                                                ('ACTIVE', '활성'),
                                                ('INACTIVE', '비활성');

CREATE TABLE banner_action_code (
                                    banner_action_code_id INT PRIMARY KEY AUTO_INCREMENT,
                                    code VARCHAR(30) NOT NULL UNIQUE,
                                    name VARCHAR(50) NOT NULL
);

INSERT INTO banner_action_code (code, name) VALUES
                                                ('CREATE', '생성'),
                                                ('UPDATE', '수정'),
                                                ('DELETE', '삭제'),
                                                ('PRIORITY_CHANGE', '우선순위 변경');

CREATE TABLE admin_role_code (
                                 admin_role_code_id INT PRIMARY KEY AUTO_INCREMENT,
                                 code VARCHAR(20) NOT NULL UNIQUE,
                                 name VARCHAR(50) NOT NULL
);

INSERT INTO admin_role_code (code, name) VALUES
                                             ('SUPER_ADMIN', '슈퍼 관리자'),
                                             ('EVENT_ADMIN', '이벤트 관리자');

CREATE TABLE access_action_code (
                                    access_action_code_id INT PRIMARY KEY AUTO_INCREMENT,
                                    code VARCHAR(30) NOT NULL UNIQUE,
                                    name VARCHAR(50) NOT NULL
);

INSERT INTO access_action_code (code, name) VALUES
                                                ('LOGIN', '로그인'),
                                                ('LOGOUT', '로그아웃'),
                                                ('SCAN', 'QR 스캔'),
                                                ('FAIL_LOGIN', '로그인 실패'),
                                                ('RESET_PASSWORD', '비밀번호 재설정');

CREATE TABLE inquiry_status_code (
                                     inquiry_status_code_id INT PRIMARY KEY AUTO_INCREMENT,
                                     code VARCHAR(20) NOT NULL UNIQUE,
                                     name VARCHAR(50) NOT NULL
);

INSERT INTO inquiry_status_code (code, name) VALUES
                                                 ('WAITING', '대기중'),
                                                 ('ANSWERED', '답변 완료');

CREATE TABLE inquiry_type_code (
                                   inquiry_type_code_id INT PRIMARY KEY AUTO_INCREMENT,
                                   code VARCHAR(20) NOT NULL UNIQUE,
                                   name VARCHAR(50) NOT NULL
);

INSERT INTO inquiry_type_code (code, name) VALUES
                                               ('GENERAL', '일반 문의'),
                                               ('EVENT', '행사 정보 문의'),
                                               ('PAYMENT', '결제 문의'),
                                               ('REFUND', '환불 요청'),
                                               ('RESERVATION', '예약 관련 문의'),
                                               ('ACCOUNT', '계정 관련 문의');

CREATE TABLE qr_check_status_code (
                                      qr_check_status_code_id INT PRIMARY KEY AUTO_INCREMENT,
                                      code VARCHAR(20) NOT NULL UNIQUE,
                                      name VARCHAR(50) NOT NULL
);

INSERT INTO qr_check_status_code (code, name) VALUES
                                                  ('CHECKIN', '입장'),
                                                  ('CHECKOUT', '퇴장');

CREATE TABLE booth_payment_status_code (
                                           booth_payment_status_code_id INT PRIMARY KEY AUTO_INCREMENT,
                                           code VARCHAR(20) NOT NULL UNIQUE,
                                           name VARCHAR(50) NOT NULL
);

INSERT INTO booth_payment_status_code (code, name) VALUES
                                                       ('PENDING', '결제 전'),
                                                       ('PAID', '결제 완료'),
                                                       ('CANCELLED', '부스 신청 취소');


CREATE TABLE booth_application_status_code (
                                               booth_application_status_code_id INT PRIMARY KEY AUTO_INCREMENT,
                                               code VARCHAR(20) NOT NULL UNIQUE,
                                               name VARCHAR(50) NOT NULL
);

INSERT INTO booth_application_status_code (code, name) VALUES
                                                           ('PENDING', '대기'),
                                                           ('APPROVED', '승인'),
                                                           ('REJECTED', '반려');


CREATE TABLE users (
                       user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       name VARCHAR(50) NOT NULL,
                       role_code_id INT NOT NULL,
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       deleted_at DATETIME DEFAULT NULL,
                       FOREIGN KEY (role_code_id) REFERENCES user_role_code(user_role_code_id)

);

CREATE TABLE client (
                        user_id BIGINT PRIMARY KEY,
                        nickname VARCHAR(50) NOT NULL,
                        phone VARCHAR(20),
                        FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE event_admin (
                             user_id BIGINT PRIMARY KEY,
                             business_number VARCHAR(20) NOT NULL,
                             contact_number VARCHAR(20),
                             contact_email VARCHAR(100),
                             active BOOLEAN NOT NULL DEFAULT FALSE,
                             FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE event_apply (
                             event_apply_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                             status_code_id INT NOT NULL,
                             event_email VARCHAR(100) NOT NULL,
                             business_number VARCHAR(20) NOT NULL,
                             verified BOOLEAN NOT NULL DEFAULT FALSE,
                             manager_name VARCHAR(50) NOT NULL,
                             email VARCHAR(100) NOT NULL,
                             contact_number VARCHAR(20) NOT NULL,
                             title_kr VARCHAR(200) NOT NULL,
                             title_eng VARCHAR(200) NOT NULL,
                             file_url VARCHAR(512) NOT NULL,
                             apply_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                             admin_comment TEXT,
                             status_updated_at DATETIME,

                             FOREIGN KEY (status_code_id) REFERENCES apply_status_code(apply_status_code_id)
);



CREATE TABLE event (
                       event_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       manager_id BIGINT,
                       status_code_id INT NOT NULL,
                       event_code VARCHAR(50) NOT NULL UNIQUE,
                       title_kr VARCHAR(200) NOT NULL,
                       title_eng VARCHAR(200) NOT NULL,
                       hidden BOOLEAN NOT NULL DEFAULT TRUE,
                       FOREIGN KEY (manager_id) REFERENCES event_admin(user_id),
                       FOREIGN KEY (status_code_id) REFERENCES event_status_code(event_status_code_id)
);

CREATE TABLE file (
                      file_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                      event_id BIGINT,
                      event_apply_id BIGINT,
                      file_url VARCHAR(512) NOT NULL,
                      referenced BOOLEAN NOT NULL,
                      file_type VARCHAR(50) NOT NULL,
                      directory VARCHAR(100),
                      original_file_name VARCHAR(100) NOT NULL,
                      stored_file_name VARCHAR(255) NOT NULL,
                      file_size BIGINT NOT NULL,
                      thumbnail BOOLEAN NOT NULL DEFAULT FALSE,
                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      FOREIGN KEY (event_id) REFERENCES event(event_id),
                      FOREIGN KEY (event_apply_id) REFERENCES event_apply(event_apply_id)
);

CREATE TABLE location (
                          location_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          address VARCHAR(255) NOT NULL,
                          building_name VARCHAR(255),
                          latitude DECIMAL(10,7) NOT NULL,
                          longitude DECIMAL(10,7) NOT NULL,
                          placeUrl VARCHAR(512)
);

CREATE TABLE category_group (
                                group_id INT PRIMARY KEY,
                                group_name VARCHAR(50) NOT NULL
);

CREATE TABLE category (
                          category_id INT PRIMARY KEY,
                          group_id INT,
                          category_name VARCHAR(100) NOT NULL,
                          FOREIGN KEY (group_id) REFERENCES category_group(group_id)
);

CREATE TABLE event_detail (
                              event_detail_id BIGINT PRIMARY KEY,
                              location_id BIGINT,
                              location_detail VARCHAR(255),
                              host_name VARCHAR(255),
                              contact_info TEXT,
                              bio VARCHAR(255),
                              content TEXT NOT NULL,
                              policy TEXT NOT NULL,
                              official_url VARCHAR(255),
                              event_time INT,
                              created_at DATETIME NOT NULL,
                              updated_at DATETIME NOT NULL,
                              thumbnail_url VARCHAR(255),
                              start_date DATE NOT NULL,
                              end_date DATE NOT NULL,
                              main_category INT NOT NULL,
                              sub_category INT NOT NULL,
                              region_code_id INT NOT NULL,
                              reentry_allowed BOOLEAN NOT NULL DEFAULT TRUE,
                              check_out_allowed BOOLEAN NOT NULL DEFAULT FALSE,
                              FOREIGN KEY (event_detail_id) REFERENCES event(event_id),
                              FOREIGN KEY (location_id) REFERENCES location(location_id),
                              FOREIGN KEY (main_category) REFERENCES category_group(group_id),
                              FOREIGN KEY (sub_category) REFERENCES category(category_id),
                              FOREIGN KEY (region_code_id) REFERENCES region_code(region_code_id)
);

CREATE TABLE event_version (
                               event_version_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               event_id BIGINT,
                               version_number INT NOT NULL,
                               snapshot JSON,
                               updated_by BIGINT NOT NULL,
                               updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               UNIQUE (event_id, version_number),
                               FOREIGN KEY (event_id) REFERENCES event(event_id)
);



CREATE TABLE external_link (
                               link_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               event_id BIGINT,
                               url VARCHAR(500) NOT NULL,
                               display_text VARCHAR(50),
                               FOREIGN KEY (event_id) REFERENCES event(event_id)
);

CREATE TABLE wishlist (
                          wishlist_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          user_id BIGINT NOT NULL,
                          event_id BIGINT NOT NULL,
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          deleted BOOLEAN NOT NULL DEFAULT FALSE,
                          UNIQUE (user_id, event_id),
                          FOREIGN KEY (user_id) REFERENCES users(user_id),
                          FOREIGN KEY (event_id) REFERENCES event(event_id)
);

CREATE TABLE ticket (
                        ticket_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(100) NOT NULL,
                        description VARCHAR(100),
                        ticket_status_code_id INT,
                        stock INT,
                        price INT NOT NULL,
                        max_purchase INT,
                        types ENUM('EVENT', 'BOOTH') NOT NULL,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        visible BOOLEAN NOT NULL DEFAULT FALSE,
                        deleted BOOLEAN NOT NULL DEFAULT FALSE,

                        FOREIGN KEY (ticket_status_code_id) REFERENCES ticket_status_code(ticket_status_code_id)
);

CREATE TABLE event_ticket (
                              ticket_id BIGINT NOT NULL,
                              event_id BIGINT NOT NULL,
                              PRIMARY KEY (ticket_id, event_id),
                              FOREIGN KEY (ticket_id) REFERENCES ticket(ticket_id),
                              FOREIGN KEY (event_id) REFERENCES event(event_id)
);

CREATE TABLE booth_ticket (
                              ticket_id BIGINT NOT NULL,
                              booth_id BIGINT NOT NULL,
                              PRIMARY KEY (ticket_id, booth_id),
                              FOREIGN KEY (ticket_id) REFERENCES ticket(ticket_id),
                              FOREIGN KEY (booth_id) REFERENCES booth(booth_id)
);

CREATE TABLE ticket_version (
                                ticket_version_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                ticket_id BIGINT,
                                version_number INT NOT NULL,
                                snapshot JSON,
                                updated_by BIGINT NOT NULL,
                                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                UNIQUE (ticket_id, version_number),
                                FOREIGN KEY (ticket_id) REFERENCES ticket(ticket_id)
);

CREATE TABLE event_schedule (
                                schedule_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                event_id BIGINT NOT NULL,
                                date DATE NOT NULL,
                                start_time TIME NOT NULL,
                                end_time TIME NOT NULL,
                                weekday INT,
                                types ENUM('EVENT', 'BOOTH') NOT NULL,
                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                FOREIGN KEY (event_id) REFERENCES event(event_id)
);

CREATE TABLE schedule_ticket (
                                 schedule_id BIGINT NOT NULL,
                                 ticket_id BIGINT NOT NULL,
                                 remaining_stock INT NOT NULL,
                                 sales_start_at DATETIME NOT NULL,
                                 sales_end_at DATETIME NOT NULL,
                                 visible BOOLEAN NOT NULL DEFAULT FALSE,

                                 PRIMARY KEY (schedule_id, ticket_id),
                                 FOREIGN KEY (schedule_id) REFERENCES event_schedule(schedule_id),
                                 FOREIGN KEY (ticket_id) REFERENCES ticket(ticket_id)
);


CREATE TABLE reservation (
                             reservation_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                             user_id BIGINT NOT NULL,
                             event_id BIGINT NOT NULL,
                             ticket_id BIGINT NOT NULL,
                             schedule_id BIGINT,
                             reservation_status_code_id INT NOT NULL,
                             quantity INT NOT NULL CHECK (quantity > 0),
                             price INT NOT NULL CHECK (price >= 0),
                             created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                             updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             canceled BOOLEAN NOT NULL DEFAULT FALSE,
                             canceled_at DATETIME DEFAULT NULL,
                             FOREIGN KEY (user_id) REFERENCES users(user_id),
                             FOREIGN KEY (event_id) REFERENCES event(event_id),
                             FOREIGN KEY (ticket_id) REFERENCES ticket(ticket_id),
                             FOREIGN KEY (schedule_id) REFERENCES event_schedule(schedule_id),
                             FOREIGN KEY (reservation_status_code_id) REFERENCES reservation_status_code(reservation_status_code_id)
);

CREATE TABLE reservation_log (
                                 reservation_log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                 reservation_id BIGINT NOT NULL,
                                 reservation_status_code_id INT NOT NULL,
                                 changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 changed_by VARCHAR(100) NOT NULL,
                                 FOREIGN KEY (reservation_id) REFERENCES reservation(reservation_id),
                                 FOREIGN KEY (reservation_status_code_id) REFERENCES reservation_status_code(reservation_status_code_id)
);

CREATE TABLE attendee (
                          attendee_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          reservation_id BIGINT NOT NULL,
                          name VARCHAR(100) NOT NULL,
                          phone VARCHAR(20),
                          email VARCHAR(100),
                          birth DATE,
                          checked_in BOOLEAN NOT NULL DEFAULT FALSE,
                          attendee_type_code_id INT NOT NULL,
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                          FOREIGN KEY (reservation_id) REFERENCES reservation(reservation_id),
                          FOREIGN KEY (attendee_type_code_id) REFERENCES attendee_type_code(attendee_type_code_id),

                          UNIQUE (reservation_id, email)
);



CREATE TABLE payment (
                         payment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                         reservation_id BIGINT NOT NULL,
                         amount INT NOT NULL CHECK (amount >= 0),
                         payment_type_code_id INT NOT NULL,
                         payment_status_code_id INT NOT NULL,
                         paid_at DATETIME DEFAULT NULL,
                         FOREIGN KEY (reservation_id) REFERENCES reservation(reservation_id),
                         FOREIGN KEY (payment_type_code_id) REFERENCES payment_type_code(payment_type_code_id),
                         FOREIGN KEY (payment_status_code_id) REFERENCES payment_status_code(payment_status_code_id)
);

CREATE TABLE refund_request (
                                refund_request_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                reservation_id BIGINT NOT NULL,
                                refund_status_code_id INT NOT NULL,
                                reason TEXT NOT NULL,
                                requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                approved_at DATETIME DEFAULT NULL,
                                refunded_at DATETIME DEFAULT NULL,
                                FOREIGN KEY (reservation_id) REFERENCES reservation(reservation_id),
                                FOREIGN KEY (refund_status_code_id) REFERENCES refund_status_code(refund_status_code_id)
);

CREATE TABLE qr_ticket (
                           qr_ticket_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                           attendee_id BIGINT NOT NULL,
                           ticket_id BIGINT NOT NULL,
                           event_id BIGINT NOT NULL,
                           expired_at DATETIME NOT NULL,
                           issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           active BOOLEAN NOT NULL DEFAULT TRUE,
                           reentry_allowed BOOLEAN NOT NULL DEFAULT FALSE,
                           qr_code VARCHAR(255) UNIQUE,
                           manual_code VARCHAR(15) UNIQUE,
                           FOREIGN KEY (attendee_id) REFERENCES attendee(attendee_id),
                           FOREIGN KEY (ticket_id,event_id) REFERENCES event_ticket(ticket_id,event_id)
);

CREATE TABLE qr_log (
                        qr_log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        qr_id BIGINT NOT NULL,
                        qr_action_code_id INT NOT NULL,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (qr_id) REFERENCES qr_ticket(qr_ticket_id),
                        FOREIGN KEY (qr_action_code_id) REFERENCES qr_action_code(qr_action_code_id)
);

CREATE TABLE admin_account (
                               admin_account_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               email VARCHAR(100) NOT NULL UNIQUE,
                               password VARCHAR(255) NOT NULL,
                               name VARCHAR(50) NOT NULL,
                               active BOOLEAN NOT NULL DEFAULT TRUE,
                               admin_role_code_id INT NOT NULL,
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               FOREIGN KEY (admin_role_code_id) REFERENCES admin_role_code(admin_role_code_id)
);

CREATE TABLE banner (
                        banner_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        title VARCHAR(100) NOT NULL,
                        image_url VARCHAR(255) NOT NULL,
                        link_url VARCHAR(255),
                        priority INT NOT NULL DEFAULT 0,
                        start_date DATETIME NOT NULL,
                        end_date DATETIME NOT NULL,
                        banner_status_code_id INT NOT NULL,
                        FOREIGN KEY (banner_status_code_id) REFERENCES banner_status_code(banner_status_code_id)
);

CREATE TABLE banner_log (
                            banner_log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            banner_id BIGINT NOT NULL,
                            changed_by BIGINT NOT NULL,
                            banner_action_code_id INT NOT NULL,
                            changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (banner_id) REFERENCES banner(banner_id),
                            FOREIGN KEY (changed_by) REFERENCES admin_account(admin_account_id),
                            FOREIGN KEY (banner_action_code_id) REFERENCES banner_action_code(banner_action_code_id)
);

CREATE TABLE system_setting (
                                system_setting_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                config_key VARCHAR(100) NOT NULL UNIQUE,
                                config_value TEXT NOT NULL,
                                description VARCHAR(255),
                                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE access_log (
                            access_log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            user_id BIGINT,
                            admin_id BIGINT,
                            address VARCHAR(45) NOT NULL,
                            access_action_code_id INT NOT NULL,
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (user_id) REFERENCES users(user_id),
                            FOREIGN KEY (admin_id) REFERENCES admin_account(admin_account_id),
                            FOREIGN KEY (access_action_code_id) REFERENCES access_action_code(access_action_code_id)
);

CREATE TABLE reservation_stats_log (
                                       reservation_stats_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                       event_id BIGINT NOT NULL,
                                       stat_date DATE NOT NULL,
                                       reservation_count INT NOT NULL DEFAULT 0 CHECK (reservation_count >= 0),
                                       cancellation_count INT NOT NULL DEFAULT 0 CHECK (cancellation_count >= 0),
                                       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       FOREIGN KEY (event_id) REFERENCES event(event_id)
);

CREATE TABLE sales_stats_log (
                                 sales_stats_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                 event_id BIGINT NOT NULL,
                                 stat_date DATE NOT NULL,
                                 refund_sales INT NOT NULL DEFAULT 0 CHECK (refund_sales >= 0),
                                 total_sales INT NOT NULL DEFAULT 0 CHECK (total_sales >= 0),
                                 service_fee INT NOT NULL DEFAULT 0 CHECK (service_fee >= 0),
                                 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 FOREIGN KEY (event_id) REFERENCES event(event_id)
);

CREATE TABLE email_verification (
                                    email VARCHAR(100) PRIMARY KEY UNIQUE NOT NULL,
                                    code VARCHAR(10) NOT NULL,
                                    expires_at DATETIME NOT NULL,
                                    verified BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE inquiry (
                         inquiry_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                         user_id BIGINT NOT NULL,
                         title VARCHAR(200) NOT NULL,
                         content TEXT NOT NULL,
                         created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                         updated_at DATETIME,
                         inquiry_status_code_id INT NOT NULL,
                         inquiry_type_code_id INT,
                         deleted TINYINT(1) NOT NULL DEFAULT 0,
                         answered TINYINT(1) NOT NULL DEFAULT 0,
                         FOREIGN KEY (user_id) REFERENCES users(user_id),
                         FOREIGN KEY (inquiry_status_code_id) REFERENCES inquiry_status_code(inquiry_status_code_id),
                         FOREIGN KEY (inquiry_type_code_id) REFERENCES inquiry_type_code(inquiry_type_code_id)
);

CREATE TABLE inquiry_reply (
                               inquiry_reply_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               inquiry_id BIGINT NOT NULL,
                               admin_id BIGINT NOT NULL,
                               content TEXT NOT NULL,
                               created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                               updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               deleted BOOLEAN NOT NULL DEFAULT 0,
                               FOREIGN KEY (inquiry_id) REFERENCES inquiry(inquiry_id),
                               FOREIGN KEY (admin_id) REFERENCES admin_account(admin_account_id)
);

CREATE TABLE share_ticket (
                              share_ticket_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                              reservation_id BIGINT NOT NULL,
                              link_token VARCHAR(255) NOT NULL UNIQUE,
                              submitted_count INT DEFAULT 0,
                              total_allowed INT NOT NULL,
                              expired BOOLEAN DEFAULT 0,
                              created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              expired_at DATETIME DEFAULT NULL,
                              FOREIGN KEY (reservation_id) REFERENCES reservation(reservation_id)
);

CREATE TABLE qr_check_log (
                              qr_check_log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                              qr_id BIGINT NOT NULL,
                              check_status_code_id INT NOT NULL,
                              created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                              FOREIGN KEY (qr_id) REFERENCES qr_ticket(qr_ticket_id),
                              FOREIGN KEY (check_status_code_id) REFERENCES qr_check_status_code(qr_check_status_code_id)
);

CREATE TABLE review (
                        review_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        reservation_id BIGINT,
                        user_id BIGINT,
                        comment TEXT NOT NULL,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME,
                        star INT NOT NULL,
                        public BOOLEAN DEFAULT TRUE,

                        FOREIGN KEY (reservation_id) REFERENCES reservation(reservation_id),
                        FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE review_reaction (
                                 review_id BIGINT NOT NULL,
                                 user_id BIGINT NOT NULL,

                                 CONSTRAINT review_reaction_id PRIMARY KEY (review_id, user_id),

                                 FOREIGN KEY (review_id) REFERENCES review(review_id),
                                 FOREIGN KEY (user_id) REFERENCES users(user_id)
);


CREATE TABLE booth (
                       booth_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       event_id BIGINT NOT NULL,
                       booth_type_id BIGINT NOT NULL,
                       booth_admin_id BIGINT,
                       booth_title VARCHAR(100) NOT NULL,
                       booth_description TEXT NOT NULL,
                       start_date DATE NOT NULL,
                       end_date DATE NOT NULL,
                       location VARCHAR(100),
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                       FOREIGN KEY (event_id) REFERENCES event(event_id),
                       FOREIGN KEY (booth_type_id) REFERENCES booth_type(booth_type_id),
                       FOREIGN KEY (booth_admin_id) REFERENCES booth_admin(user_id)
);

CREATE TABLE booth_type (
                            booth_type_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            name VARCHAR(100) NOT NULL,
                            size VARCHAR(50),
                            price INT NOT NULL CHECK (price >= 0),
                            max_applicants INT CHECK (max_applicants IS NULL OR max_applicants >= 0)
);

CREATE TABLE booth_admin (
                             user_id BIGINT PRIMARY KEY,
                             manager_name VARCHAR(20) NOT NULL,
                             email VARCHAR(100) NOT NULL UNIQUE,
                             contact_number VARCHAR(20) NOT NULL,
                             official_url VARCHAR(512) NOT NULL,

                             FOREIGN KEY (user_id) REFERENCES users(user_id)
);


CREATE TABLE booth_application (
                                   booth_application_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                   event_id BIGINT NOT NULL,
                                   booth_email VARCHAR(100) NOT NULL,
                                   booth_title VARCHAR(100) NOT NULL,
                                   booth_description TEXT,
                                   manager_name VARCHAR(20) NOT NULL,
                                   email VARCHAR(100) NOT NULL,
                                   contact_number VARCHAR(20) NOT NULL,
                                   official_url VARCHAR(512) NOT NULL,
                                   apply_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                   admin_comment TEXT,
                                   booth_application_status_code_id INT NOT NULL,
                                   booth_payment_status_code_id INT NOT NULL DEFAULT 1, -- 1 = PENDING
                                   status_updated_at DATETIME,
                                   FOREIGN KEY (event_id) REFERENCES event(event_id),
                                   FOREIGN KEY (booth_application_status_code_id) REFERENCES booth_application_status_code(booth_application_status_code_id),
                                   FOREIGN KEY (booth_payment_status_code_id) REFERENCES booth_payment_status_code(booth_payment_status_code_id)
);
