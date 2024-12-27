from flask import Flask, request, jsonify
import mysql.connector
from flask_cors import CORS
from datetime import datetime
import json

def get_current_time():
    # Get the current date and time
    current_time = datetime.now()
    # Format the time as a string in the desired format
    formatted_time = current_time.strftime("%Y-%m-%d %H:%M:%S")
    return formatted_time

# Initialize the Flask app first
app = Flask(__name__)

# Enable CORS
CORS(app)

# Configure the database
db_config = {
    'host': 'localhost',
    'user': 'root',
    'password': 'thecookapp',
    'database': 'recipes_app'
}

# Ensure the connection to the database
def get_db_connection():
    return mysql.connector.connect(**db_config)


@app.route('/add_recipe', methods=['POST'])
def add_recipe():
    data = request.json

    # Extract data fields from the request
    user_id = data.get('user_id')
    post_id = data.get('post_id')  # Assume post_id is generated or passed in the request
    title = data.get('title')
    description = data.get('description')
    #ingredients = data.get('ingredients')
    ingredients_json = json.dumps(data.get("ingredients", {}))
    instructions_json = json.dumps(data.get("instructions", []))
    instructions = data.get('instructions')
    image_url = data.get('image_url')
    difficulty = data.get('difficulty', '')  # Default to empty string if not provided
    servings = data.get('servings', 0)     # Default to 0 servings if not provided
    time = get_current_time() #data.get('time', '')            # Default to empty string if not provided

    # Connect to the database
    db = get_db_connection()
    cursor = db.cursor()

    # SQL query to insert a new recipe (post)
    sql = """
        INSERT INTO posts (user_id, post_id, title, description, ingredients, instructions, image_url, difficulty, servings, time)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """

    # Values to be inserted into the table
    values = (
        user_id,
        post_id,  # Assuming post_id will be generated or passed in request
        title,
        description,
        ingredients_json,
        instructions_json,
        image_url,
        difficulty,
        servings,
        time
    )

    try:
        cursor.execute(sql, values)
        db.commit()

        # Return the ID of the newly inserted post and a success message
        return jsonify({"id": cursor.lastrowid, "message": "Recipe added successfully!"}), 201
    except mysql.connector.Error as err:
        db.rollback()  # Rollback in case of error
        return jsonify({"error": f"Failed to add recipe: {err}"}), 500
    finally:
        cursor.close()
        db.close()  # Ensure the connection is closed after the operation

@app.route('/get_post_count/<string:user_id>', methods=['GET'])
def get_post_count(user_id):
    """
    Fetches the count of posts for a specific user.

    Args:
        user_id: The ID of the user passed as a path parameter.

    Returns:
        A JSON response containing the post count for the specified user.
    """
    try:
        # Query the database to get the post count for the user
        db = get_db_connection()
        cursor = db.cursor()
        query = "SELECT COUNT(*) FROM posts WHERE user_id = %s"
        cursor.execute(query, (user_id,))
        count = cursor.fetchone()[0]
        return str(count), 200

    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5000, debug=True)

