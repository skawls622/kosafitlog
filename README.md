[README.md](https://github.com/user-attachments/files/32881309/README.md)
# kosaFitLog!

> 개인 운동 루틴과 실제 운동 기록을 하나의 흐름으로 관리하는  
> **Spring Boot 기반 반응형 운동 기록 웹 애플리케이션**

헬스장에서 미리 구성한 루틴을 선택하고, 세트가 끝날 때마다 중량과 반복 횟수를 기록한 뒤  
누적된 운동 기록과 체성분 변화를 확인할 수 있도록 구현했습니다.

<p>
  <img src="https://img.shields.io/badge/Java-11-007396?logo=openjdk&logoColor=white" alt="Java 11">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7.18-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot 2.7.18">
  <img src="https://img.shields.io/badge/MyBatis-3.x-000000" alt="MyBatis 3.x">
  <img src="https://img.shields.io/badge/Oracle-XE%2021c-F80000?logo=oracle&logoColor=white" alt="Oracle XE 21c">
  <img src="https://img.shields.io/badge/Thymeleaf-Template-005F0F?logo=thymeleaf&logoColor=white" alt="Thymeleaf">
</p>

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|---|---|
| 프로젝트명 | **kosaFitLog!** |
| 개발 형태 | 1인 개인 프로젝트 |
| 개발 기간 | 2026.09.14 ~ 2026.09.30 |
| 주요 목적 | 운동 루틴 → 실제 운동 기록 → 통계/체성분 확인을 하나의 흐름으로 연결 |
| 사용 환경 | PC / 스마트폰 웹 브라우저 |
| 형태 | 반응형 웹 애플리케이션 |

### 실제 사용 흐름

```text
헬스장에서 운동 시작
        ↓
미리 구성한 루틴 선택
        ↓
한 세트 직후 중량 / 반복 횟수 기록
        ↓
운동 기록 누적
        ↓
통계 및 체성분 변화 확인
```

---

## 2. 주요 기능

### 회원
- 회원가입
- 로그인 / 로그아웃
- `HttpSession` 기반 로그인 상태 관리
- BCrypt 기반 비밀번호 암호화

### 운동 루틴
- 루틴 등록 / 조회 / 수정 / 삭제
- 루틴에 여러 운동 추가
- 운동 순서 관리
- 운동별 메모 관리

### 운동 기록
- 저장된 루틴을 기반으로 운동 시작
- 운동별 세트 기록
- 중량 / 반복 횟수 저장
- 세트 삭제
- 운동 완료 처리
- 전체 운동 기록 및 날짜별 기록 조회

### 통계
- 운동 기록 기반 통계 조회
- 운동량 변화 차트 제공

### 체성분
- 체성분 기록 등록 / 조회 / 삭제
- 체성분 변화 차트 제공

### 운동 정보
- API Ninjas를 이용한 운동 정보 조회
- DeepL API를 이용한 한글 번역
- 번역 실패 시 영문 원문 유지

---

## 3. 기술 스택

### Backend
- Java 11
- Spring Boot 2.7.18
- Spring Framework 5.3.x
- Spring MVC
- MyBatis 3.x
- Maven

### Database
- Oracle XE 21c
- Oracle Sequence

### Frontend
- Thymeleaf
- HTML
- CSS
- JavaScript
- Responsive Web

### Development Tools
- STS
- DBeaver
- Git / GitHub

---

## 4. Backend Architecture

```text
Browser
   ↓
Controller
   ↓
Service
   ↓
Mapper + Mapper XML
   ↓
Oracle Database
```

| 계층 | 역할 |
|---|---|
| Controller | 요청 처리, Session 확인, 화면 이동 |
| Service | 비즈니스 로직, 검증, Transaction 처리 |
| Mapper + XML | SQL 실행 및 DB 접근 |
| Oracle | 실제 데이터 저장 |

---

## 5. Database

주요 테이블은 총 8개입니다.

```text
MEMBERS
 ├─ ROUTINES
 │   └─ ROUTINE_EXERCISES
 │
 ├─ WORKOUT_LOGS
 │   └─ WORKOUT_EXERCISES
 │       └─ WORKOUT_SETS
 │
 └─ BODY_COMPOSITIONS

EXERCISES
```

---

## 6. 핵심 설계

### 6-1. Plan과 Record 분리

운동 루틴은 이후 수정될 수 있는 **계획(Plan)** 이고,  
운동 기록은 이미 수행한 **과거 이력(Record)** 이라고 구분했습니다.

운동 시작 시점에 루틴의 운동 구성 정보를 실제 기록 영역으로 복사합니다.

```text
ROUTINES
    ↓
ROUTINE_EXERCISES
    ↓
  운동 시작
    ↓
WORKOUT_LOGS
    ↓
WORKOUT_EXERCISES
```

운동 시작 시 복사하는 정보:
- 운동 ID
- 운동 순서
- 메모

> 현재 구조에서는 `WORKOUT_LOGS`에 `routine_id`를 저장하지 않기 때문에  
> 어느 루틴에서 시작한 운동인지는 직접 추적하지 않습니다.

### 6-2. 운동 시작을 하나의 Transaction으로 처리

사용자에게는 **운동 시작 버튼 한 번**이지만 DB에서는 여러 번의 INSERT가 발생합니다.

중간 INSERT에서 예외가 발생했을 때 일부 데이터만 저장되는 상황을 줄이기 위해  
`startWorkout()` 전체를 `@Transactional`로 처리했습니다.

```java
@Transactional
public Long startWorkout(Long memberId, Long routineId) {
    // 소유 루틴 확인
    // 이번 운동 기록 생성
    // 루틴의 운동 구성을 실제 운동 기록으로 복사
}
```

### 6-3. Oracle Sequence + MyBatis selectKey

자식 운동 구성 데이터를 저장하려면 먼저 부모 운동 기록의 PK가 필요합니다.

Oracle Sequence와 MyBatis `selectKey BEFORE`를 사용해 부모 ID를 먼저 확보한 뒤  
같은 ID를 자식 데이터에 전달하도록 구현했습니다.

```xml
<selectKey keyProperty="workoutLogId"
           resultType="long"
           order="BEFORE">
    SELECT SEQ_WORKOUT_LOGS.NEXTVAL FROM DUAL
</selectKey>
```

### 6-4. 사용자 데이터 소유권 확인

로그인 여부만 확인한다고 해서 현재 요청한 데이터가 로그인 사용자의 데이터라는 보장은 없습니다.

따라서 데이터 번호만 조회하지 않고  
**Session에 저장된 로그인 사용자 ID를 함께 조회 조건으로 사용**했습니다.

```sql
WHERE workout_log_id = #{workoutLogId}
  AND member_id = #{memberId}
```

사용자를 식별하는 `memberId`는 클라이언트 Request 값이 아니라  
서버의 Session 로그인 정보에서 가져옵니다.

---

## 7. Troubleshooting

### MyBatis ResultMap property 매핑 오류

운동 정보 조회 과정에서 다음 오류가 발생했습니다.

```text
There is no setter for property named 'apikeyword'
```

DB 컬럼과 Java DTO는 다음과 같았습니다.

```text
DB Column : api_keyword
Java DTO  : apiKeyword
```

잘못된 ResultMap:

```xml
<result column="api_keyword" property="apikeyword"/>
```

수정 후:

```xml
<result column="api_keyword" property="apiKeyword"/>
```

- `column` = DB 조회 결과 컬럼
- `property` = Java 객체의 프로퍼티

---

## 8. Screenshots

아래 경로에 실제 화면 이미지를 추가하면 README에 바로 표시할 수 있습니다.

```text
docs/
└─ images/
   ├─ routine.png
   ├─ workout.png
   ├─ statistics.png
   └─ body-composition.png
```

예시:

```markdown
![운동 루틴](docs/images/routine.png)
![운동 기록](docs/images/workout.png)
![운동 통계](docs/images/statistics.png)
![체성분](docs/images/body-composition.png)
```

---

## 9. 구현 과정에서 중점적으로 고민한 부분

1. 수정 가능한 운동 계획과 이미 수행한 운동 기록의 분리
2. 하나의 사용자 동작에서 발생하는 여러 DB 작업의 Transaction 처리
3. Oracle Sequence를 이용한 부모 / 자식 ID 처리
4. Session 사용자 정보와 객체 ID를 함께 사용한 데이터 소유권 확인
5. MyBatis Mapper와 DTO 사이의 데이터 매핑

---

## 10. 현재 구조의 한계와 개선 방향

- `WORKOUT_LOGS`에 원본 루틴 ID가 없어 어떤 루틴에서 시작했는지 직접 추적하지 않음
- 운동명 / 운동 부위는 별도 snapshot이 아니라 `EXERCISES` JOIN으로 조회
- 로그인과 접근 제어는 `HttpSession`과 Interceptor 중심으로 구현
- 실제 Oracle DB를 이용한 Transaction rollback 통합 테스트는 추가 가능
- 외부 운동 정보 및 번역 기능은 외부 API 상태에 영향을 받을 수 있음

---

## Repository

https://github.com/skawls622/kosafitlog

---

## Author

**권남진**
