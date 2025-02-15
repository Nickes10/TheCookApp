from flask import Flask, request, jsonify
import mysql.connector
from flask_cors import CORS
from datetime import datetime, timedelta
import json
import os
from werkzeug.utils import secure_filename
import decimal

'''Flask Web API uploaded in PythonAnyWhere service'''

# Initialize the Flask app first
app = Flask(__name__)
CORS(app)

UPLOAD_FOLDER = '/home/Nick99/mysite/recipesImages'  # Replace with your desired upload folder
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

# Configure the database
db_config = {
    'host': 'Nick99.mysql.pythonanywhere-services.com',
    'user': 'Nick99',
    'password': 'thecookapp',
    'database': 'Nick99$recipes_app'
}

# Global log container
logs = ['Hello']

def add_log(message):
    logs.append(message)
    return message

def get_current_time():
    current_time = datetime.now()
    formatted_time = current_time.strftime("%Y-%m-%d %H:%M:%S")
    return formatted_time

@app.route('/health', methods=['GET'])
def health_check():
    add_log("Connection established.")
    return jsonify({'success': True, 'message': 'Connected to Flask server'}), 200

# Ensure the connection to the database
def get_db_connection():
    try:
        connection = mysql.connector.connect(**db_config)
        add_log("Database connection established.")
        return connection
    except mysql.connector.Error as err:
        add_log(f"Database connection error: {err}")
        raise


@app.route('/upload', methods=['POST'])
def upload_image():
    if 'image' not in request.files:
        return jsonify({'error': 'No image part in the request'}), 400

    file = request.files['image']
    if file.filename == '':
        return jsonify({'error': 'No selected file'}), 400

    if file:
        filename = secure_filename(file.filename)
        file_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
        file.save(file_path)
        public_url = f"http://Nick99.pythonanywhere.com/static/{filename}"
        return jsonify({'message': 'Image uploaded successfully', 'url': public_url}), 201


@app.route('/add_recipe', methods=['POST'])
def add_recipe():
    data = request.json
    add_log("Received data: " + json.dumps(data))

    user_id = data.get('user_id')
    post_id = data.get('post_id')
    title = data.get('title')
    description = data.get('description')
    ingredients_json = json.dumps(data.get("ingredients", {}))
    instructions_json = json.dumps(data.get("instructions", []))
    image_url = data.get('image_url')
    difficulty = data.get('difficulty', '')
    servings = data.get('servings', 0)
    time_to_do = data.get('time_to_do', 0)
    recipe_position = data.get('recipe_position', None)  # New field replacing latitude/longitude

    try:
        connection = get_db_connection()
        cursor = connection.cursor()

        sql = """
            INSERT INTO posts (user_id, post_id, title, description, ingredients, instructions, image_url, difficulty, servings, time_to_do, recipe_position)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """

        values = (
            user_id,
            post_id,
            title,
            description,
            ingredients_json,
            instructions_json,
            image_url,
            difficulty,
            servings,
            time_to_do,
            recipe_position
        )

        cursor.execute(sql, values)
        connection.commit()
        add_log("Recipe added successfully with ID: " + str(cursor.lastrowid))
        return jsonify({"id": cursor.lastrowid, "message": "Recipe added successfully!"}), 201

    except mysql.connector.Error as err:
        add_log(f"Database operation failed: {err}")
        return jsonify({"error": f"Failed to add recipe: {err}"}), 500

    finally:
        try:
            cursor.close()
            connection.close()
            add_log("Database connection closed.")
        except Exception as e:
            add_log(f"Error closing connection: {e}")


@app.route('/get_post_count/<string:user_id>', methods=['GET'])
def get_post_count(user_id):
    """
    Fetches the count of posts for a specific user.

    Args:
        user_id: The ID of the user passed as a path parameter.

    Returns:
        A JSON response containing the post count for the specified user.
    """
    add_log("I enter in the get_post_count")
    try:
        # Query the database to get the post count for the user
        db = get_db_connection()
        cursor = db.cursor()
        query = "SELECT COUNT(*) FROM posts WHERE user_id = %s"
        cursor.execute(query, (user_id,))
        count = cursor.fetchone()[0]
        add_log("ok finish get_post_count")
        return str(count), 200

    except Exception as e:
        return jsonify({'error': str(e)}), 500

def custom_serializer(obj):
    if isinstance(obj, datetime):
        return obj.isoformat()  # Convert datetime to ISO 8601 string
    elif isinstance(obj, timedelta):
        # Convert timedelta to a string representing total seconds or a formatted string
        return str(obj)  # Example: "1 day, 2:30:00"
    elif isinstance(obj, decimal.Decimal):
        return float(obj) # convert decimal to float
    raise TypeError(f"Type {type(obj)} not serializable")

@app.route('/get_post/<string:user_id>', methods=['GET'])
def get_post(user_id):
    if not user_id:
        return jsonify({"error": "user_id parameter is required"}), 400

    connection = get_db_connection()
    cursor = connection.cursor(dictionary=True)

    try:
        # Fetch posts from the database
        cursor.execute('SELECT * FROM posts WHERE user_id = %s', (user_id,))
        posts = cursor.fetchall()

        if posts:
            for post in posts:
                # Deserialize 'ingredients' and 'instructions'
                if isinstance(post.get("ingredients"), str):
                    post["ingredients"] = json.loads(post["ingredients"])
                if isinstance(post.get("instructions"), str):
                    post["instructions"] = json.loads(post["instructions"])

                # Convert 'created_at' to datetime object
                if "created_at" in post and isinstance(post["created_at"], str):
                    post["created_at"] = datetime.strptime(post["created_at"], "%Y-%m-%d %H:%M:%S")

                # Include `recipe_position` in the response
                post["recipe_position"] = post.get("recipe_position", "Unknown Location")

            # Serialize posts with the custom serializer
            return json.dumps(posts, default=custom_serializer), 200
        else:
            return jsonify({"message": "No posts found for this user"}), 404
    except Exception as e:
        return jsonify({"error": str(e)}), 500
    finally:
        cursor.close()
        connection.close()


@app.route('/update_post/<string:user_id>/<int:post_id>', methods=['PUT'])
def update_post(user_id, post_id):
    data = request.json
    add_log(f"Updating post with user_id: {user_id}, post_id: {post_id}")

    fields = {
        "title": data.get("title"),
        "description": data.get("description"),
        "ingredients": json.dumps(data.get("ingredients", {})),
        "instructions": json.dumps(data.get("instructions", [])),
        "image_url": data.get("image_url"),
        "difficulty": data.get("difficulty"),
        "servings": data.get("servings"),
        "time_to_do": data.get("time_to_do"),
        "recipe_position": data.get("recipe_position")  # Replaces latitude & longitude
    }

    updates = ", ".join([f"{key} = %s" for key in fields if fields[key] is not None])
    values = tuple(fields[key] for key in fields if fields[key] is not None)

    if not updates:
        return jsonify({"error": "No fields to update"}), 400

    try:
        connection = get_db_connection()
        cursor = connection.cursor()

        query = f"UPDATE posts SET {updates} WHERE user_id = %s AND post_id = %s"
        values += (user_id, post_id)
        cursor.execute(query, values)
        connection.commit()

        if cursor.rowcount == 0:
            add_log(f"No post found with user_id: {user_id}, post_id: {post_id}")
            return jsonify({"message": "Post not found"}), 404

        add_log(f"Post with user_id: {user_id}, post_id: {post_id} updated successfully.")
        return jsonify({"message": "Post updated successfully!"}), 200

    except mysql.connector.Error as err:
        add_log(f"Failed to update post: {err}")
        return jsonify({"error": f"Failed to update post: {err}"}), 500

    finally:
        try:
            cursor.close()
            connection.close()
        except Exception as e:
            add_log(f"Error closing connection: {e}")


@app.route('/delete_post/<string:user_id>/<int:post_id>', methods=['DELETE'])
def delete_post(user_id, post_id):
    add_log(f"Deleting post with user_id: {user_id}, post_id: {post_id}")

    try:
        connection = get_db_connection()
        cursor = connection.cursor()

        query = "DELETE FROM posts WHERE user_id = %s AND post_id = %s"
        cursor.execute(query, (user_id, post_id))
        connection.commit()

        if cursor.rowcount == 0:
            add_log(f"No post found with user_id: {user_id}, post_id: {post_id}")
            return jsonify({"message": "Post not found"}), 404

        add_log(f"Post with user_id: {user_id}, post_id: {post_id} deleted successfully.")
        return jsonify({"message": "Post deleted successfully!"}), 200

    except mysql.connector.Error as err:
        add_log(f"Failed to delete post: {err}")
        return jsonify({"error": f"Failed to delete post: {err}"}), 500

    finally:
        try:
            cursor.close()
            connection.close()
        except Exception as e:
            add_log(f"Error closing connection: {e}")

@app.route('/delete_account/<string:user_id>', methods=['DELETE'])
def delete_account(user_id):
    add_log(f"Deleting all posts with user_id: {user_id}")

    try:
        connection = get_db_connection()
        cursor = connection.cursor()

        query = "DELETE FROM posts WHERE user_id = %s"
        cursor.execute(query, (user_id,))  # Ensure the parameter is passed as a tuple
        connection.commit()

        if cursor.rowcount == 0:
            add_log(f"No posts found with user_id: {user_id}")
            return jsonify({"success": False, "message": "No posts found with the given user ID"}), 404

        add_log(f"Posts with user_id: {user_id} deleted successfully.")
        return jsonify({"success": True, "message": "Posts deleted successfully!"}), 200

    except mysql.connector.Error as err:
        add_log(f"Failed to delete posts: {err}")
        return jsonify({"success": False, "error": f"Failed to delete posts: {err}"}), 500

    finally:
        try:
            cursor.close()
            connection.close()
        except Exception as e:
            add_log(f"Error closing connection: {e}")


@app.route('/')
def view_logs():
    return '<br>'.join(logs)

